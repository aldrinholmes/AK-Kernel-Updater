package lb.themike10452.hellscorekernelupdater;

import android.content.Context;

/**
 * Created by Mike on 9/20/2014.
 */
public class Keys {

    //public static String DEFAULT_SOURCE = "http://pastebin.com/download.php?i=G6xhB7x9";
    public static String DEFAULT_SOURCE = "http://pastebin.com/download.php?i=4Cvf7eqS";
    public static String SOURCE_CODE = "https://github.com/themike10452/HellsCore_Kernel_Updater";
    public static String TAG_NOTIF = "THEMIKE10452.HKU.UPDNOTIF";

    public static String KEY_SETTINGS_SOURCE = "_SOURCE";
    public static String KEY_SETTINGS_DOWNLOADLOCATION = "_DLLOCATION";
    public static String KEY_SETTINGS_USEANDM = "_USEANDDM";
    public static String KEY_SETTINGS_AUTOCHECK_ENABLED = "_ENABLEBAC";
    public static String KEY_SETTINGS_AUTOCHECK_INTERVAL = "_BACINTERVAL";
    public static String KEY_SETTINGS_ROMBASE = "_ROMBASE";
    public static String KEY_SETTINGS_USESTATICFILENAME = "_USESTATICFILENAME";
    public static String KEY_SETTINGS_LASTSTATICFILENAME = "_STATICFILENAME";

    private static String KEY_KERNEL_VERSION = "_version(%s):=";
    private static String KEY_KERNEL_ZIPNAME = "_zipname(%s):=";
    private static String KEY_KERNEL_HTTPLINK = "_httplink(%s):=";
    private static String KEY_KERNEL_MD5 = "_md5(%s):=";

    public static String getKEY_KERNEL_VERSION(Context c) {
        return String.format(KEY_KERNEL_VERSION, c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS).getString(KEY_SETTINGS_ROMBASE, "null")).toLowerCase();
    }

    public static String getKEY_KERNEL_ZIPNAME(Context c) {
        return String.format(KEY_KERNEL_ZIPNAME, c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS).getString(KEY_SETTINGS_ROMBASE, "null")).toLowerCase();
    }

    public static String getKEY_KERNEL_HTTPLINK(Context c) {
        return String.format(KEY_KERNEL_HTTPLINK, c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS).getString(KEY_SETTINGS_ROMBASE, "null")).toLowerCase();
    }

    public static String getKEY_KERNEL_MD5(Context c) {
        return String.format(KEY_KERNEL_MD5, c.getSharedPreferences("Settings", Context.MODE_MULTI_PROCESS).getString(KEY_SETTINGS_ROMBASE, "null")).toLowerCase();
    }
}
