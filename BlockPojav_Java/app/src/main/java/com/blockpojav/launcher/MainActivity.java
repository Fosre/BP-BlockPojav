package com.blockpojav.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

/** Independent Java UI foundation. Game process/runtime is not integrated yet. */
public final class MainActivity extends Activity {
    private final int bg = Color.rgb(10,12,16), panel = Color.rgb(23,27,35), accent = Color.rgb(83,190,255), white = Color.rgb(240,244,250), muted = Color.rgb(155,166,182);
    private android.content.SharedPreferences prefs;
    private TextView ramText, status;
    private Spinner versions;
    private int maxRam;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        prefs = getSharedPreferences("blockpojav", MODE_PRIVATE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        ((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(info);
        maxRam = Math.max(1, (int)(info.availMem / (1024L*1024L*1024L)) - 1);
        render();
    }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18)); return d; }
    private TextView label(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(null,1); return t; }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(24),dp(20),dp(28)); scroll.addView(root); setContentView(scroll);
        root.addView(label("BLOCKPOJAV • JAVA ALPHA",14,accent,true));
        TextView title=label("Твой Minecraft. Твой лаунчер.",27,white,true); title.setPadding(0,dp(12),0,dp(5)); root.addView(title);
        root.addView(label("Самостоятельный проект BlockPojav",14,muted,false)); gap(root,20);
        EditText nick=new EditText(this); nick.setSingleLine(true); nick.setTextColor(white); nick.setHintTextColor(muted); nick.setHint("Игровой ник"); nick.setText(prefs.getString("nick","")); nick.setBackground(shape(panel)); nick.setPadding(dp(14),0,dp(14),0); root.addView(nick,new LinearLayout.LayoutParams(-1,dp(54)));
        gap(root,14); versions=new Spinner(this); String[] vs={"1.20.1","1.20.4","1.20.6","1.21.1","1.21.4"}; ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,vs); versions.setAdapter(a); versions.setSelection(Math.max(0,a.getPosition(prefs.getString("version","1.20.1")))); root.addView(versions);
        gap(root,18); ramText=label("Память игры: "+prefs.getInt("ram",Math.min(2,maxRam))+" ГБ",16,white,true); root.addView(ramText);
        SeekBar bar=new SeekBar(this); bar.setMax(Math.max(0,maxRam-1)); bar.setProgress(Math.max(0,Math.min(bar.getMax(),prefs.getInt("ram",Math.min(2,maxRam))-1))); root.addView(bar); bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} public void onProgressChanged(SeekBar s,int p,boolean u){ramText.setText("Память игры: "+(p+1)+" ГБ");}});
        gap(root,14); Button low=button("Для слабого устройства"); root.addView(low); low.setOnClickListener(v->{bar.setProgress(0); status.setText("Включён щадящий профиль памяти. Графические настройки появятся при интеграции запуска.");});
        gap(root,20); status=label("Java-интерфейс готов. Установка файлов, Runtime, графический запуск и вход Microsoft пока не подключены.",14,muted,false); status.setPadding(dp(14),dp(14),dp(14),dp(14)); status.setBackground(shape(panel)); root.addView(status);
        gap(root,16); Button save=button("Сохранить настройки"); root.addView(save); save.setOnClickListener(v->{prefs.edit().putString("nick",nick.getText().toString()).putString("version",versions.getSelectedItem().toString()).putInt("ram",bar.getProgress()+1).apply(); status.setText("Настройки сохранены на этом устройстве.");});
    }
    private Button button(String s){Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(white); b.setBackground(shape(panel)); return b;}
    private void gap(LinearLayout l,int h){ android.view.View v=new android.view.View(this); l.addView(v,new LinearLayout.LayoutParams(1,dp(h))); }
}
