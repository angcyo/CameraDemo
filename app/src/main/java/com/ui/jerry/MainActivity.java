package com.ui.jerry;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    Bitmap bitmap;
    ImageView imageView;
    private StartTakePhotoView startTakePhotoView;

    public static ImageHandle handle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startTakePhotoView = (StartTakePhotoView) findViewById(R.id.startTakePhotoView);
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(800, 800);
//
//        startTakePhotoView.setLayoutParams(params);

        imageView = (ImageView) findViewById(R.id.image);
        imageView.setRotation(90);

        handle = new ImageHandle();
    }

    public void toCameraClicked(View view) {
        //启动之前释放当前camera
        startTakePhotoView.releaseCamera();
        CameraManager.getInstance(this).openCameraActivity(this);
    }


    public class ImageHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 111) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                bitmap = (Bitmap) msg.obj;
                imageView.setImageBitmap(bitmap);

                Log.e("angcyo", "---------------------");
            }
        }
    }

}
