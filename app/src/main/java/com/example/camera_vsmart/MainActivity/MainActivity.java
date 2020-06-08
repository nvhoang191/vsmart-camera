package com.example.camera_vsmart.MainActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.camera_vsmart.CameraActivity.CameraScreen;
import com.example.camera_vsmart.R;
import com.example.camera_vsmart.Utils.AppConstants;
import com.example.camera_vsmart.Utils.DebugLog;
import com.example.camera_vsmart.Utils.PermissionHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnOpenCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        initEvents();
        if(!PermissionHelper.requestRuntimePermission(this)){
            DebugLog.d("MainActivity: !PermissionHelper.requestRuntimePermission");
        }
    }

    private void initComponents() {
        btnOpenCamera = findViewById(R.id.btn_open_camera);
    }

    private void initEvents() {
        btnOpenCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d("Camera Button", "Pressed");
        switchToCameraActivity();
    }

    private void switchToCameraActivity() {
        startActivity(new Intent(this, CameraScreen.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case AppConstants.Common.COMMON_REQUEST_PERMISSION_CODE: {
                //boolean permissionAllowed = true;
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        //TODO Define a toast here
                        //permissionAllowed = false;
                        finish();
                    }
                }
            }
        }
    }
}
