package com.example.camera_vsmart;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.camera_vsmart.CameraActivity.CameraScreen;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Thủy ơi giờ t có main activity này rồi, có button click vào là trỏ sang màn camera, ô xem giúp t nhé
    private Button btnOpenCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initComponents();
        initEvents();
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



//    private void openDefaultCamera() {
//        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_IMAGE_CAPTURE) {
//            Bitmap photo = (Bitmap)data.getExtras().get("data");
//
//            // Set the image in imageview for display
//            ivPicture.setImageBitmap(photo);
//        }
//    }
}
