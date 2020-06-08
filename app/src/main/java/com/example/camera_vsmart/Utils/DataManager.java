package com.example.camera_vsmart.Utils;

import android.content.Context;
import android.util.Size;

import java.util.Hashtable;

/**
 * Singleton pattern
 */

public class DataManager {
    private static volatile DataManager sInstance = null;
    private Context mContext = null;

    private Size mScreenResolution = null;
    private Hashtable<String, String> mExifImage = null;

    private DataManager() {

    }

    public synchronized static DataManager getInstance() {
        if (sInstance == null) {
            DebugLog.d("Create instance");
            sInstance = new DataManager();
            return sInstance;
        }
        return sInstance;
    }

    public void setContext(Context context) {
        this.mContext = context;
        DebugLog.d("SetContext:" + this.mContext);
    }

    public synchronized void setScreenResolution(Size size) {
        mScreenResolution = size;
    }

    public synchronized Size getScreenResolution() {
        return mScreenResolution;
    }


    public synchronized void setExifImage(Hashtable<String, String> hash) {
        mExifImage = hash;
    }

    public synchronized Hashtable<String, String> getExifImage() {
        return mExifImage;
    }
}
