package com.example.camera_vsmart.CameraActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.camera_vsmart.R;
import com.example.camera_vsmart.Utils.AppUtils;
import com.example.camera_vsmart.Utils.DataManager;
import com.example.camera_vsmart.Utils.DebugLog;

public class PreviewScreen extends AppCompatActivity {

    private ImageView mImageView;
    private TextView mTvScreen;
    private TextView mTvFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preview);
        initComponents();
        displayImage();
    }

    private void initComponents() {
        mImageView = findViewById(R.id.iv_preview_photo);
        mTvScreen = findViewById(R.id.tv_screen_resolution);
        mTvFrame = findViewById(R.id.tv_frame_resolution);
    }

    private void displayImage() {
        Intent intent = getIntent();
        String picName = intent.getStringExtra("imgName");
        Bitmap bitmap = BitmapFactory.decodeFile(AppUtils.getFileDir(picName));

        DebugLog.d(AppUtils.getFileDir(picName));

        mImageView.setImageBitmap(bitmap);

        Size screenSize = DataManager.getInstance().getScreenResolution();
        DataManager.getInstance().setScreenResolution(null);
        mTvScreen.setText("Screen resolution: " + screenSize.toString());

        //Size screenFrame = DataManager.getInstance().getFrameResolution();
        //DataManager.getInstance().setScreenResolution(null);
        //mTvFrame.setText("Frame resolution: " + screenFrame.toString());
    }
}
