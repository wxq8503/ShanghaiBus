package com.alan.shanghaibus;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * <p>Created 16/1/15 下午10:59.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public class Config {
    static final boolean debug = true;
    private static final String PREFERENCE_NAME = "config";

    static final String KEY_USER_AGENT_LIST = "KEY_USER_AGENT_LIST";

    private static final String KEY_NOTIFICATION_SERVICE_ENABLE = "KEY_NOTIFICATION_SERVICE_ENABLE";

    private static final String KEY_NOTIFY_SOUND = "KEY_NOTIFY_SOUND";
    private static final String KEY_NOTIFY_VIBRATE = "KEY_NOTIFY_VIBRATE";
    private static final String KEY_NOTIFY_NIGHT_ENABLE = "KEY_NOTIFY_NIGHT_ENABLE";

    static final String KEY_BUS_NO = "KEY_BUS_NO";
    static final String KEY_BUS_ENABLE_ALL = "KEY_BUS_ENABLE_ALL";
    static final String KEY_BUS_STOP_ID = "KEY_BUS_STOP_ID";
    static final String KEY_BUS_DIRECTIONS_LIST = "KEY_BUS_DIRECTIONS_LIST";

    private static final String KEY_AGREEMENT = "KEY_AGREEMENT";

    private static Config current;

    public static synchronized Config getConfig(Context context) {
        if(current == null) {
            current = new Config(context.getApplicationContext());
        }
        return current;
    }

    private SharedPreferences preferences;
    private Context mContext;

    private Config(Context context) {
        mContext = context;
        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /** 是否启动通知栏模式*/
    public boolean isEnableNotificationService() {
        return preferences.getBoolean(KEY_NOTIFICATION_SERVICE_ENABLE, false);
    }

    public void setNotificationServiceEnable(boolean enable) {
        preferences.edit().putBoolean(KEY_NOTIFICATION_SERVICE_ENABLE, enable).apply();
    }

    /** 是否开启声音*/
    public boolean isNotifySound() {
        return preferences.getBoolean(KEY_NOTIFY_SOUND, true);
    }

    /** 是否开启震动*/
    public boolean isNotifyVibrate() {
        return preferences.getBoolean(KEY_NOTIFY_VIBRATE, true);
    }

    /** 是否开启夜间免打扰模式*/
    public boolean isNotifyNight() {
        return preferences.getBoolean(KEY_NOTIFY_NIGHT_ENABLE, false);
    }

    /** 免费声明*/
    public boolean isAgreement() {
        return preferences.getBoolean(KEY_AGREEMENT, false);
    }

    /** 设置是否同意*/
    public void setAgreement(boolean agreement) {
        preferences.edit().putBoolean(KEY_AGREEMENT, agreement).apply();
    }

}
