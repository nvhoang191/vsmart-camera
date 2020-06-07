package com.example.camera_vsmart;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * SharedPreferences Utilities
 */

public class PreferencesUtil {
    private PreferencesUtil() {
    }

    public static void putString(Context context, String key, String value) {
        DebugLog.d("<< putString >> key:" + key + " value:" + value);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.edit().putString(key, value).apply();
    }

    public static String getString(Context context, String key, String defValue) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return mPrefs.getString(key, defValue);
    }

    public static void putLong(Context context, String key, long value) {
        DebugLog.d("<< putLong >> key:" + key + " value:" + value);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.edit().putLong(key, value).apply();
    }

    public static long getLong(Context context, String key, long defValue) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return mPrefs.getLong(key, defValue);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        DebugLog.d("<< putBoolean >> key:" + key + " value:" + value);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return mPrefs.getBoolean(key, defValue);
    }

    public static void putInt(Context context, String key, int value) {
        DebugLog.d("<< putInt >> key:" + key + " value:" + value);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.edit().putInt(key, value).apply();
    }

    public static int getInt(Context context, String key, int defValue) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return mPrefs.getInt(key, defValue);
    }

    public static void remove(Context context, String key) {
        DebugLog.d("<< remove >> key:" + key);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.edit().remove(key).apply();
    }

    public static void getDefaultSharedPreferences(Context context) {
        Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(context).getAll();
        for (String key : prefs.keySet()) {
            Object pref = prefs.get(key);
            String printVal = "";
            if (pref instanceof Boolean) {
                printVal = key + " : " + (Boolean) pref;
                DebugLog.i(printVal);
            }
            if (pref instanceof Float) {
                printVal = key + " : " + (Float) pref;
                DebugLog.i(printVal);
            }
            if (pref instanceof Integer) {
                printVal = key + " : " + (Integer) pref;
                DebugLog.i(printVal);
            }
            if (pref instanceof Long) {
                printVal = key + " : " + (Long) pref;
                DebugLog.i(printVal);
            }
            if (pref instanceof String) {
                printVal = key + " : " + (String) pref;
                DebugLog.i(printVal);
            }
            if (pref instanceof Set<?>) {
                printVal = key + " : " + (Set<String>) pref;
                DebugLog.i(printVal);
            }
        }
    }

    public static void putIntArray(Context context, String key, int[] array) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            str.append(array[i]).append(",");
        }
        mPrefs.edit().putString(key, String.valueOf(str)).apply();
    }

    public static int[] getIntArray(Context context, String key, int[] defValue) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedString = mPrefs.getString(key, defValue.toString());

        DebugLog.e("save string:" + savedString);
        StringTokenizer st = new StringTokenizer(savedString, ",");
        //int[] savedList = new int[10];
        List<Integer> saveList = new ArrayList<>();
        while (st.hasMoreElements()) {
            saveList.add(Integer.parseInt(st.nextToken()));
        }
        int[] array = toIntArray(saveList);
        return array;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e.intValue();
        return ret;
    }

}
