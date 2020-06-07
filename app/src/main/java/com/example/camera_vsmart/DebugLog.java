package com.example.camera_vsmart;

import android.util.Log;

import java.util.regex.Pattern;

public class DebugLog {
    private static boolean sDebug_MODE_ENABLE = true;
    private final static String PREFIX_TAG = "CAMERA_VSMART";

    public static void d(String message) {
        Log.d(getTag(), message);
    }

    public static void d(String message, Object... args) {
    }

    public static void i(String message, Object... args) {
    }

    public static void i(String message) {
        Log.i(getTag(), message);
    }

    public static void w(String message) {
        Log.w(getTag(), message);
    }

    public static void w(String message, Throwable e) {
        Log.w(getTag(), message, e);
    }

    public static void e(String message) {
        Log.e(getTag(), message);
    }

    public static void e(String message, Throwable e) {
        Log.e(getTag(), message, e);
    }

    private static String getTag() {
        StackTraceElement elm = new Throwable().getStackTrace()[2];
        if (elm == null) {
            return PREFIX_TAG;
        }
        Pattern pkgPattern = Pattern.compile(".*\\.");
        String className = pkgPattern.matcher(elm.getClassName()).replaceAll("");
        String methodName = elm.getMethodName();
        int line = elm.getLineNumber();

        return PREFIX_TAG + "|" + className + "#" + methodName + "(" + line + ")";
    }
}