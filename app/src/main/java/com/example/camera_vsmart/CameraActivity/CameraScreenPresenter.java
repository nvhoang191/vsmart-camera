package com.example.camera_vsmart.CameraActivity;

import android.content.Context;
import android.graphics.Rect;

public class CameraScreenPresenter implements ICameraContract.IPresenterAction {
    private Context mContext;
    private ICameraContract.IViewListener mViewListener;

    CameraScreenPresenter(Context context, ICameraContract.IViewListener viewListener) {
        this.mContext = context;
        this.mViewListener = viewListener;
    }

    @Override
    public void openCamera() {

    }

    @Override
    public void closeCamera() {

    }

    @Override
    public void startBackgroundThread() {

    }

    @Override
    public void stopBackgroundThread() {

    }

    @Override
    public void takePhoto() {

    }

    @Override
    public void setFlashOff() {

    }

    @Override
    public void setFlashOn() {

    }

    @Override
    public void setFlashAuto() {

    }

    @Override
    public void regionsFocus(Rect touchRect) {

    }
}
