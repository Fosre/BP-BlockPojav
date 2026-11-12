package com.blockpojav.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/** Downloads official vanilla version metadata, client, libraries and assets. Does not launch Minecraft. */
final class MinecraftInstaller {
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    interface Progress { void update(String message); }
    interface Finished { void done(Exception error); }
    private MinecraftInstaller() {}

    static boolean isInstalled(File root, String version) {
        File dir = new File(root, "versions/" + version);
        File meta = new File(dir, version + ".json"), jar = new File(dir, version + ".jar"), marker = new File(dir, "installed.ok");
        if (!meta.isFile() || !jar.isFile() || !marker.isFile()) return false;
        try { return version.equals(new JSONObject(read(meta)).getString("id")); }
        catch (Exception e) { return false; }
    }

    static void install(File root, String version, Progress progress, Finished finished) {
        new Thread(() -> {
            Exception error = null;
            try { installNow(root, version, progress); }
            catch (Exception e) { error = e; }
            Exception result = error;
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            main.post(() -> finished.done(result));
        }, "BlockPojav-Install").start();
    }

    private static void installNow(File root, String version, Progress progress) throws Exception {
        if (!version.matches("[0-9]+\\.[0-9]+(\\.[0-9]+)?")) throw new IOException("Неверный номер версии");
        if (!root.mkdirs() && !root.isDirectory()) throw new IOException("Не удалось создать папку Minecraft");
        progress.update("Проверяю официальный список версий…");
        JSONObject manifest = new JSONObject(readUrl(MANIFEST));
        JSONArray versions = manifest.getJSONArray("versions");
        JSONObject entry = null;
        for (int i=0; i<versions.length(); i++) if (version.equals(versions.getJSONObject(i).getString("id"))) { entry=versions.getJSONObject(i); break; }
        if (entry == null) throw new IOException("Версия " + version + " не найдена в официальном списке Mojang");
        File dir = new File(root,"versions/"+version); dir.mkdirs();
        File metaFile = new File(dir,version+".json");
        download(entry.getString("url"),metaFile,entry.optString("sha1"));
        JSONObject meta = new JSONObject(read(metaFile));
        progress.update("Загружаю клиент Minecraft…");
        JSONObject client = meta.getJSONObject("downloads").getJSONObject("client");
        download(client.getString("url"),new File(dir,version+".jar"),client.optString("sha1"));
        JSONArray libs = meta.optJSONArray("libraries");
        if (libs != null) for (int i=0;i<libs.length();i++) {
            JSONObject lib=libs.getJSONObject(i);
            if (!allowed(lib.optJSONArray("rules"))) continue;
            JSONObject artifact=lib.optJSONObject("downloads") == null ? null : lib.getJSONObject("downloads").optJSONObject("artifact");
            if (artifact==null || artifact.optString("path").isEmpty()) continue;
            progress.update("Библиотеки " +(i+1)+" из "+libs.length()+"…");
            download(artifact.getString("url"),new File(root,"libraries/"+artifact.getString("path")),artifact.optString("sha1"));
        }
        JSONObject index=meta.optJSONObject("assetIndex");
        if(index!=null) {
            progress.update("Загружаю индекс ресурсов…");
            String id=index.getString("id"); File indexFile=new File(root,"assets/indexes/"+id+".json");
            download(index.getString("url"),indexFile,index.optString("sha1"));
            JSONObject objects=new JSONObject(read(indexFile)).getJSONObject("objects");
            Iterator<String> names=objects.keys(); List<JSONObject> batch=new ArrayList<>(); ExecutorService pool=Executors.newFixedThreadPool(6);
            try {
                while(names.hasNext()) {
                    JSONObject obj=objects.getJSONObject(names.next()); String hash=obj.getString("hash");
                    File target=new File(root,"assets/objects/"+hash.substring(0,2)+"/"+hash);
                    if(!target.isFile() || target.length()!=obj.getLong("size")) batch.add(new JSONObject().put("hash",hash).put("target",target));
                    if(batch.size()>=24) { fetchBatch(pool,batch,progress); batch.clear(); }
                }
                if(!batch.isEmpty()) fetchBatch(pool,batch,progress);
            } finally { pool.shutdownNow(); }
        }
        try (FileOutputStream marker = new FileOutputStream(new File(dir,"installed.ok"))) { marker.write("vanilla-files-v1".getBytes("UTF-8")); }
    }

    private static void fetchBatch(ExecutorService pool,List<JSONObject> batch,Progress progress) throws Exception {
        List<Future<?>> futures=new ArrayList<>();
        for(JSONObject o:batch) { String hash=o.getString("hash"); File target=(File)o.get("target");
            futures.add(pool.submit(() -> { try { download("https://resources.download.minecraft.net/"+hash.substring(0,2)+"/"+hash,target,hash); } catch(Exception e) { throw new CompletionException(e); } })); }
        progress.update("Загружаю ресурсы…");
        for(Future<?> f:futures) try { f.get(); } catch(ExecutionException e) { Throwable c=e.getCause(); if(c instanceof CompletionException && c.getCause() instanceof Exception) throw (Exception)c.getCause(); throw e; }
    }
    private static boolean allowed(JSONArray rules) throws Exception {
        if(rules==null)return true; boolean allow=false;
        for(int i=0;i<rules.length();i++){ JSONObject r=rules.getJSONObject(i); JSONObject os=r.optJSONObject("os"); if(os==null || "linux".equals(os.optString("name"))) allow="allow".equals(r.optString("action")); }
        return allow;
    }
    private static void copy(InputStream in, OutputStream out)throws IOException { byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1) out.write(b,0,n); }
    private static String readUrl(String url)throws Exception { HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(60000); try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()){ copy(in,out); return out.toString("UTF-8"); } finally { c.disconnect(); } }
    private static String read(File f)throws Exception { try(FileInputStream in=new FileInputStream(f); ByteArrayOutputStream out=new ByteArrayOutputStream()){ copy(in,out); return out.toString("UTF-8"); } }
    private static void download(String url,File target,String sha)throws Exception {
        target.getParentFile().mkdirs();
        if(target.isFile() && sha!=null && !sha.isEmpty() && sha.equalsIgnoreCase(sha1(target)))return;
        File tmp=new File(target.getParentFile(),target.getName()+".part"); HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(20000); c.setReadTimeout(60000);
        try { int status=c.getResponseCode(); if(status<200||status>=300)throw new IOException("HTTP "+status+" при загрузке файла"); try(InputStream in=c.getInputStream(); FileOutputStream out=new FileOutputStream(tmp)){ copy(in,out); } } finally { c.disconnect(); }
        if(sha!=null&&!sha.isEmpty()&&!sha.equalsIgnoreCase(sha1(tmp))){tmp.delete();throw new IOException("Проверка файла не прошла: "+target.getName());}
        if(target.exists())target.delete(); if(!tmp.renameTo(target))throw new IOException("Не удалось сохранить "+target.getName());
    }
    private static String sha1(File file)throws Exception { MessageDigest d=MessageDigest.getInstance("SHA-1"); try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);} StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format("%02x",x));return s.toString(); }
}
