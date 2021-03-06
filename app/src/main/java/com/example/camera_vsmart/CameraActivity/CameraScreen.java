package com.example.camera_vsmart.CameraActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.camera_vsmart.Utils.AppConstants;
import com.example.camera_vsmart.Utils.DataManager;
import com.example.camera_vsmart.Utils.DebugLog;
import com.example.camera_vsmart.R;

public class CameraScreen extends AppCompatActivity implements ICameraContract.IViewListener, View.OnClickListener {

    private ICameraContract.IPresenterAction mPresenter;
    private ImageButton mBtnTakePhoto;
    private TextureView mTvCameraPreview;
    private ImageButton mBtnFlashMode;
    private ImageButton mBtnBack;
//    private ImageView mIvFrameCamera;
//    private TextView mTvFrameRatio;

    private int mFlashMode;
    private boolean mCameraOpened;
//    private int mCurrentFrame;

    private boolean mCaptureCompleted;
    private boolean mWritePicCompleted;

    private static final int TIME_WAIT_CAPTURE = 3;
    private boolean mClickedTakePhoto;
    private boolean mRegionsFocusable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initComponents();
        initEvents();
    }

    private void hiddenStatusBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void initComponents() {
        mTvCameraPreview = findViewById(R.id.tv_camera_preview);
        mPresenter = new CameraScreenPresenter(this, this);
        mBtnTakePhoto = findViewById(R.id.btn_take_photo);
        mBtnFlashMode = findViewById(R.id.btn_flash_mode);
        mFlashMode = AppConstants.CameraConfig.MODE_FLASH_OFF;
        mBtnBack = findViewById(R.id.btn_back);
//        mIvFrameCamera = findViewById(R.id.iv_frame_camera);
//        mTvFrameRatio = findViewById(R.id.tv_frame_ratio);
    }

    private void initEvents() {
        mBtnTakePhoto.setOnClickListener(this);
        mBtnFlashMode.setOnClickListener(this);
        mBtnBack.setOnClickListener(this);
//        mTvFrameRatio.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!mCameraOpened) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_take_photo: {
                takePhoto();
                break;
            }
            case R.id.btn_flash_mode: {
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
//            case R.id.tv_frame_ratio: {
//                setFrameCamera();
//                break;
//            }
            case R.id.btn_back: {
                this.finish();
                break;
            }
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

    @Override
    protected void onResume() {
        super.onResume();
        mCameraOpened = false;
//        mCurrentFrame = AppConstants.RatioFrame.RATIO_0;

        mPresenter.startBackgroundThread();
        if (mTvCameraPreview.isAvailable()) {
            mPresenter.openCamera();
        } else {
            mTvCameraPreview.setSurfaceTextureListener(getSurfaceTextureListener());
        }
    }

    @Override
    protected void onPause() {
//        if (mCountDownTimer != null) {
//            mCountDownTimer.cancel();
//        }
        mPresenter.closeCamera();
        mPresenter.stopBackgroundThread();
        super.onPause();
        finish();
    }

    private TextureView.SurfaceTextureListener getSurfaceTextureListener() {
        return new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mPresenter.openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                //DebugLog.d("Camera change size");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };
    }

    @Override
    public SurfaceTexture getCameraPreview() {
        return mTvCameraPreview.getSurfaceTexture();
    }

    @Override
    public void onCaptureCompleted(String imgName) {
        if (mWritePicCompleted) {
            previewPhoto(imgName);
        } else {
            mCaptureCompleted = true;
        }

    }

    @Override
    public void onWritePicCompleted(String imgName) {
        if (mCaptureCompleted) {
            previewPhoto(imgName);
        } else {
            mWritePicCompleted = true;
        }
    }

    private void previewPhoto(String imgName) {
        if (imgName != null) {
            DebugLog.d("Preview photo");
            Intent intent = new Intent(this, PreviewScreen.class);
            intent.putExtra(AppConstants.Common.IMG_NAME, imgName);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, R.string.cant_write_photo, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onConfigCameraComplete() {
        DataManager.getInstance().setScreenResolution(new Size(mTvCameraPreview.getHeight(), mTvCameraPreview.getWidth()));
        DebugLog.d("Screen resolution: " + DataManager.getInstance().getScreenResolution().toString());
        setFlash();
        mRegionsFocusable = true;
        mCameraOpened = true;
//        setFrameCamera();
//        if (PreferencesUtil.getBoolean(this, AppConstants.SharePreferenceKeys.AUTO_CAPTURE_STATE, false)) {
//            autoCapture(); // giờ t ko cần auto chụp thì comment đoạn này là đc nhỉ
//        }
    }

//    private CountDownTimer mCountDownTimer;

//    private void autoCapture() {
//        if (mCountDownTimer == null) {
//            mCountDownTimer = new CountDownTimer(TIME_WAIT_CAPTURE * 2000, 2000) {
//                @Override
//                public void onTick(long millisUntilFinished) {
//                    showText(CameraScreen.this, (millisUntilFinished / 2000) + "");
//                }
//
//                @Override
//                public void onFinish() {
//                    mPresenter.takePhoto();
//                }
//            };
//        }
//        mCountDownTimer.start();
//    }

//    private void showText(Context context, String text) {
//        LayoutInflater inflater = getLayoutInflater();
//        View layout = inflater.inflate(R.layout.custom_toast,
//                (ViewGroup) findViewById(R.id.custom_toast_container));
//
//        TextView toastText = layout.findViewById(R.id.toast_text);
//        toastText.setText(text);
//
//        Toast toast = new Toast(context);
//        toast.setGravity(Gravity.START, 50, 0);
//        toast.setDuration(Toast.LENGTH_SHORT);
//        toast.setView(layout);
//        toast.show();
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mCameraOpened) return false;
        DebugLog.d("key code: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP: {
                takePhoto();
                break;
            }
            case KeyEvent.KEYCODE_BACK: {
                this.finish();
            }
        }
        return true;
    }

//    @SuppressLint("SetTextI18n")
//    private void setFrameCamera() {
//        mCurrentFrame += 1;
//        int height;
//        int width;
//
//        switch (mCurrentFrame) {
//            case AppConstants.RatioFrame.RATIO_56x29: {
//                height = getFrameHeight(6);
//                width = (int) (height / 1.93);
//                mTvFrameRatio.setText("56x29");
//                break;
//            }
//            case AppConstants.RatioFrame.RATIO_76x30: {
//                height = getFrameHeight(4);
//                width = (int) (height / 2.53);
//                mTvFrameRatio.setText("76x30");
//                break;
//            }
//            case AppConstants.RatioFrame.RATIO_70x20: {
//                height = getFrameHeight(3);
//                width = (int) (height / 3.5);
//                mTvFrameRatio.setText("70x20");
//                break;
//            }
//            default: {
//                mCurrentFrame = AppConstants.RatioFrame.RATIO_0;
//                setFrameCamera();
//                return;
//            }
//        }
//        mIvFrameCamera.getLayoutParams().height = height;
//        mIvFrameCamera.getLayoutParams().width = width;
//        mIvFrameCamera.requestLayout();
//        DataManager.getInstance().updateFrameResolution(new Size(height, width));
//        DebugLog.d("Frame resolution: " + DataManager.getInstance().getFrameResolution().toString());
//    }

//    /**
//     * khoảng trống = indexSpace * kích thước nút chụp ảnh
//     *
//     * @param indexSpace số lần khoảng trống
//     * @return frame height
//     */
//    private int getFrameHeight(int indexSpace) {
//        return mTvCameraPreview.getHeight() - indexSpace * mBtnTakePhoto.getHeight();
//    }

    @Override
    public void onWritePicFailed() {
        finish();
        Toast.makeText(this, getResources().getString(R.string.take_photo_error), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraFailed() {
        finish();
        Toast.makeText(this, getResources().getString(R.string.camera_action_failed), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            DebugLog.d("notification pushed up");
            hiddenStatusBar();
        } else {
            DebugLog.d("notification pulled down");
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        regionsFocus();
        DebugLog.d("touched screen");
        return super.onTouchEvent(event);
    }

    private void regionsFocus() {
        if (!mRegionsFocusable) return;

        float x = (float) mTvCameraPreview.getWidth() / 2;
        float y = (float) mTvCameraPreview.getHeight() / 2;
        Rect touchRect = new Rect(
                (int) (x - 100),
                (int) (y - 100),
                (int) (x + 100),
                (int) (y + 100)
        );
        mPresenter.regionsFocus(touchRect);
    }

    private void takePhoto() {
        if (mClickedTakePhoto) return;
        mClickedTakePhoto = true;
        mPresenter.takePhoto();
    }
}
