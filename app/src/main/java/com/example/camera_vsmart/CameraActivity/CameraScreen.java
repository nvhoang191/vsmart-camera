package com.example.camera_vsmart.CameraActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.example.camera_vsmart.AppConstants;
import com.example.camera_vsmart.R;

public class CameraScreen extends AppCompatActivity implements ICameraContract.IViewListener, View.OnClickListener {

    private ICameraContract.IPresenterAction mPresenter;
    private ImageButton mBtnBack, mBtnCapture, mBtnFlashMode;
    private boolean mCameraOpened, mClickedTakePhoto;
    private int mFlashMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initComponents();
        initEvents();
    }

    private void initComponents() {
        mPresenter = new CameraScreenPresenter(this, this);

        mBtnBack = findViewById(R.id.btn_back);
        mBtnCapture = findViewById(R.id.btn_take_photo);
        mBtnFlashMode = findViewById(R.id.btn_flash_mode);
    }

    private void initEvents() {
        mBtnBack.setOnClickListener(this);
        mBtnCapture.setOnClickListener(this);
        mBtnFlashMode.setOnClickListener(this);
    }

    private void hiddenStatusBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public SurfaceTexture getCameraPreview() {
        return null;
    }

    @Override
    public void onCaptureCompleted(String imgName) {

    }

    @Override
    public void onWritePicCompleted(String imgName) {

    }

    @Override
    public void onWritePicFailed() {

    }

    @Override
    public void onConfigCameraComplete() {

    }

    @Override
    public void onCameraFailed() {

    }

    @Override
    public void onClick(View v) {
        if (!mCameraOpened)
            return;
        switch (v.getId()) {
            case R.id.btn_take_photo:
                takePhoto();
                break;

            case R.id.btn_flash_mode:
                switch (mFlashMode) {
                    case AppConstants.CameraConfig.MODE_FLASH_OFF: {
                        mFlashMode = AppConstants.CameraConfig.MODE_FLASH_ON;
                        break;
                    }

                    case AppConstants.CameraConfig.MODE_FLASH_ON: {
                        mFlashMode = AppConstants.CameraConfig.MODE_FLASH_AUTO;
                        break;
                    }

                    case AppConstants.CameraConfig.MODE_FLASH_AUTO: {
                        mFlashMode = AppConstants.CameraConfig.MODE_FLASH_OFF;
                        break;
                    }
                }
                setFlash();
                break;
        }
    }

    private void setFlash() {
        switch (mFlashMode) {
            case AppConstants.CameraConfig.MODE_FLASH_ON: {
                mBtnFlashMode.setBackgroundResource(R.drawable.ic_flash_on);
                mPresenter.setFlashOn();
                break;
            }

            case AppConstants.CameraConfig.MODE_FLASH_AUTO: {
                mBtnFlashMode.setBackgroundResource(R.drawable.ic_flash_auto);
                mPresenter.setFlashAuto();
                break;
            }

            case AppConstants.CameraConfig.MODE_FLASH_OFF: {
                mBtnFlashMode.setBackgroundResource(R.drawable.ic_flash_off);
                mPresenter.setFlashOff();
                break;
            }
        }
    }

    private void takePhoto() {
        if (mClickedTakePhoto) return;
        mClickedTakePhoto = true;
        mPresenter.takePhoto();
    }
}
