package com.blockpojav.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import java.io.File;

/** BlockPojav's own Java interface and official vanilla file installer. */
public final class MainActivity extends Activity {
    private final int bg=Color.rgb(10,12,16),panel=Color.rgb(23,27,35),accent=Color.rgb(83,190,255),white=Color.rgb(240,244,250),muted=Color.rgb(155,166,182);
    private android.content.SharedPreferences prefs; private TextView ramText,status; private Spinner versions; private int maxRam; private Button installPlay; private File gameRoot;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);prefs=getSharedPreferences("blockpojav",MODE_PRIVATE);gameRoot=getExternalFilesDir("minecraft");if(gameRoot==null)gameRoot=new File(getFilesDir(),"minecraft");ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(info);maxRam=Math.max(1,(int)(info.availMem/(1024L*1024L*1024L))-1);render();}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density);} private GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(18));return d;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;}
    private void render(){ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(24),dp(20),dp(28));scroll.addView(root);setContentView(scroll);
        root.addView(label("BLOCKPOJAV • JAVA ALPHA",14,accent,true));TextView title=label("Твой Minecraft. Твой лаунчер.",27,white,true);title.setPadding(0,dp(12),0,dp(5));root.addView(title);root.addView(label("Самостоятельный проект BlockPojav",14,muted,false));gap(root,20);
        EditText nick=new EditText(this);nick.setSingleLine(true);nick.setTextColor(white);nick.setHintTextColor(muted);nick.setHint("Игровой ник");nick.setText(prefs.getString("nick",""));nick.setBackground(shape(panel));nick.setPadding(dp(14),0,dp(14),0);root.addView(nick,new LinearLayout.LayoutParams(-1,dp(54)));
        gap(root,14);versions=new Spinner(this);String[] vs={"1.20.1","1.20.4","1.20.6","1.21.1","1.21.4"};ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,vs);versions.setAdapter(a);versions.setSelection(Math.max(0,a.getPosition(prefs.getString("version","1.20.1"))));root.addView(versions);
        gap(root,18);ramText=label("Память игры: "+prefs.getInt("ram",Math.min(2,maxRam))+" ГБ",16,white,true);root.addView(ramText);SeekBar bar=new SeekBar(this);bar.setMax(Math.max(0,maxRam-1));bar.setProgress(Math.max(0,Math.min(bar.getMax(),prefs.getInt("ram",Math.min(2,maxRam))-1)));root.addView(bar);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){ramText.setText("Память игры: "+(p+1)+" ГБ");}});
        gap(root,12);Button low=button("Для слабого устройства");root.addView(low);low.setOnClickListener(v->{bar.setProgress(0);status.setText("Щадящий лимит памяти выбран. Графические настройки появятся после подключения запуска игры.");});
        gap(root,16);installPlay=button("УСТАНОВИТЬ");root.addView(installPlay);gap(root,12);status=label("Установка скачивает официальные файлы выбранной версии. Для запуска игры ещё потребуются Runtime, графика и вход Microsoft.",14,muted,false);status.setPadding(dp(14),dp(14),dp(14),dp(14));status.setBackground(shape(panel));root.addView(status);
        versions.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?>p){}public void onItemSelected(android.widget.AdapterView<?>p,android.view.View v,int position,long id){refreshButton();}});
        installPlay.setOnClickListener(v->{String version=versions.getSelectedItem().toString();if(MinecraftInstaller.isInstalled(gameRoot,version)){new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Файлы установлены").setMessage("Minecraft пока не запускается: следующим этапом подключается среда запуска и графика.").setPositiveButton("Понятно",null).show();return;}installPlay.setEnabled(false);installPlay.setText("ЗАГРУЖАЮ…");MinecraftInstaller.install(gameRoot,version,msg->status.setText(msg),error->{installPlay.setEnabled(true);if(error==null){status.setText("Файлы "+version+" загружены и проверены. Запуск игры появится после подключения Runtime и графики.");refreshButton();}else{status.setText("Не удалось установить: "+error.getMessage());refreshButton();}});});
        gap(root,12);Button save=button("Сохранить настройки");root.addView(save);save.setOnClickListener(v->{prefs.edit().putString("nick",nick.getText().toString()).putString("version",versions.getSelectedItem().toString()).putInt("ram",bar.getProgress()+1).apply();status.setText("Настройки сохранены на этом устройстве.");});refreshButton();
    }
    private void refreshButton(){if(installPlay==null||versions.getSelectedItem()==null)return;installPlay.setText(MinecraftInstaller.isInstalled(gameRoot,versions.getSelectedItem().toString())?"ИГРАТЬ":"УСТАНОВИТЬ");}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(white);b.setBackground(shape(panel));return b;} private void gap(LinearLayout l,int h){android.view.View v=new android.view.View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
}
