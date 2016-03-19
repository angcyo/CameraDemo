package com.ui.jerry;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

/**
 * 自定义相机的activity
 *
 * @author jerry
 * @date 2015-09-01
 */
public class CameraActivity extends Activity implements OnTouchListener {
    public static final String TAG ="CameraActivity";

    public static final String PATH_SAVE_PIC = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera";

    private Camera mCamera;
    private Camera.Parameters parameters = null;
    private CameraManager mCameraManager;

    private SurfaceView surfaceView;
    private TextView m_tvFlashLight, m_tvCameraDireation;
    private LinearLayout ll_camera_container;
    private CameraView cameraView;

    private CameraManager.CameraDirection mCameraId; //0后置  1前置
    private CameraManager.FlashLigthStatus mFlashLightStatus;

    private boolean isZoom; //是否是缩放
    private float dist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraManager = CameraManager.getInstance(this);

        initView();
        initData();
        initListener();
    }

    void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        m_tvFlashLight = (TextView) findViewById(R.id.tv_flashlight);
        m_tvCameraDireation = (TextView) findViewById(R.id.tv_camera_direction);
        cameraView = (CameraView) findViewById(R.id.cameraView);
        ll_camera_container = (LinearLayout) findViewById(R.id.ll_camera_container);

        surfaceView.setFocusable(true);
        surfaceView.setBackgroundColor(TRIM_MEMORY_BACKGROUND);
        surfaceView.getHolder().addCallback(new SurfaceCallback());//为SurfaceView的句柄添加一个回调函数

        //修正surfaceView的高度
        int winWidth = getWindowManager().getDefaultDisplay().getWidth();
        int width = View.MeasureSpec.makeMeasureSpec(winWidth, View.MeasureSpec.AT_MOST);
        int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        ll_camera_container.measure(width, height);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        params.height = ll_camera_container.getMeasuredHeight();
        surfaceView.setLayoutParams(params);
    }

    void initData() {
        mCameraManager.bindView(m_tvFlashLight, m_tvCameraDireation);

        mCameraId = mCameraManager.getCameraDirection();
        //todo  获取系统相册中的一张相片
    }

    void initListener() {
        if (mCameraManager.canSwitch()) {
            m_tvCameraDireation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCamera();
                }
            });
        }

        m_tvFlashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnLight(mFlashLightStatus.next());
            }
        });

        surfaceView.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                isZoom = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                dist = spacing(event);
                if (dist > 10f) {
                    isZoom = true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                isZoom = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isZoom) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        float scale = (newDist - dist) / dist;
                        if (scale < 0) {
                            scale = scale * 10;
                        }
                        addZoomIn((int) scale);
                    }
                }
                break;
        }
        return true;
    }

    /*SurfaceCallback*/
    private final class SurfaceCallback implements SurfaceHolder.Callback {

        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mCameraManager.releaseCamera(mCamera);
            } catch (Exception e) {
                //相机已经关了
            }

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG,"surfaceCreated");

            if (null == mCamera) {
                try {
                    cameraView.init();
                    //打开默认的摄像头
                    mCamera = mCameraManager.openDefaultCamera();
                    mCamera.setPreviewDisplay(holder);
                    initCamera();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            autoFocus();
        }
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        mFlashLightStatus = mCameraManager.getLightStatus();
        turnLight(mFlashLightStatus);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCameraManager.setDispaly(mCamera);
        try {
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
//        mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }


    /**
     * 切换摄像头
     */
    private void switchCamera() {
        mCameraId = mCameraId.next();
        releaseCamera();

        setUpCamera(mCameraId.ordinal());
    }

    /**
     * 闪光灯开关   开->关->自动
     */
    private void turnLight(CameraManager.FlashLigthStatus ligthStatus) {
        if (mCamera == null || mCamera.getParameters() == null
                || mCamera.getParameters().getSupportedFlashModes() == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> supportedModes = mCamera.getParameters().getSupportedFlashModes();

        switch (ligthStatus) {
            case LIGHT_AUTO:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
            case LIGTH_OFF:
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                break;
            case LIGHT_ON:
                if (supportedModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                break;
        }
        mCamera.setParameters(parameters);
        mFlashLightStatus = ligthStatus;
        mCameraManager.setLightStatus(ligthStatus);
    }

    @Override
    public void onBackPressed() {
        releaseCamera();
        super.onBackPressed();
    }

    /**
     * 释放相机
     */
    private void releaseCamera() {
        synchronized (mCamera) {
            if (mCamera != null) {
                mCameraManager.releaseCamera(mCamera);
                mCamera = null;
            }
        }
    }

    private void setUpCamera(int facing) {
        mCamera = mCameraManager.openCameraFacing(facing);
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(surfaceView.getHolder());
                initCamera();
                mCamera.startPreview();

                mCameraManager.setCameraDirection(mCameraManager.getCameraDirection().next());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
//            toast("切换失败，请重试！", Toast.LENGTH_LONG);

        }
    }

    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    int curZoomValue = 0;

    /**
     * 对相机进行缩放
     */
    private void addZoomIn(int delta) {

        try {
            Camera.Parameters params = mCamera.getParameters();
            Log.d("Camera", "Is support Zoom " + params.isZoomSupported());
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                params.setZoom(curZoomValue);
                mCamera.setParameters(params);
                return;
            } else {
                mCamera.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 退出按钮点击
     */
    public void onExitClicked(View view) {
        onBackPressed();
    }

    /**
     * 照相按钮点击
     */
    public void onTakePhotoClicked(View view) {
        try {
            mCamera.takePicture(null, null, new MyPictureCallback());
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, R.string.topic_camera_takephoto_failure, Toast.LENGTH_LONG).show();
            try {
                mCamera.startPreview();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @return imagePath 图片路径
     * @throws IOException
     */
    public String saveToSDCard(byte[] data) throws IOException {
        //todo 保存到sd卡
        Bitmap croppedImage;

        //获取图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        //保存到SD卡

        return "";
    }

    private class MyPictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            cameraView.closeTo100();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //todo 保存相片
            new SavePicTask(data).execute();
            //重新预览
            mCamera.startPreview();
        }
    }

    private class SavePicTask extends AsyncTask<Void, Void, String> {
        private byte[] data;

        protected void onPreExecute() {
//            showProgressDialog("处理中");
        }

        SavePicTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveToSDCard(data);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

//            if (StringUtils.isNotEmpty(result)) {
////                dismissProgressDialog();
//                CameraManager.getInst().processPhotoItem(CameraActivity.this,
//                        new PhotoItem(result, System.currentTimeMillis()));
//            } else {
//                toast("拍照失败，请稍后重试！", Toast.LENGTH_LONG);
//            }
        }
    }

}
