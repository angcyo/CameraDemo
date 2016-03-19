package com.ui.jerry;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import java.io.IOException;
import java.util.Vector;

import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;

/**
 * 开始拍摄按钮
 *
 * @author jerry
 * @date 2015-09-06
 */
public class StartTakePhotoView extends SurfaceView implements SurfaceHolder.Callback,
        PreviewCallback {
    public static final String TAG = "StartTakePhotoView";

    private Camera mCamera;
    private CameraManager mCameraManager;
    private Context mContext;

    private Paint mPaint;
    private SurfaceHolder holder;
    private int srcResId;

    private boolean mCameraExist;

    private Vector<byte[]> mVector = new Vector<byte[]>();  //绘图集合
//    private Thread mDrawThread;

    public StartTakePhotoView(Context context) {
        this(context, null);
    }

    public StartTakePhotoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StartTakePhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.startTakePhotoView);
        srcResId = a.getResourceId(R.styleable.startTakePhotoView_src, -1);
        a.recycle();

        mContext = context;
        mCameraManager = CameraManager.getInstance(context);

        mPaint = new Paint();
        holder = getHolder();
        getHolder().addCallback(this);
        setBackgroundColor(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);

        setWillNotDraw(false);


//        Log.i(TAG,"main Thread pid:"+Thread.currentThread().getId());
    }

    void initCamera() {
        mCamera = mCameraManager.openCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        mCameraManager.setDispaly(mCamera);
        mCamera.setPreviewCallback(this);
        mCameraExist = true;

        Camera.Parameters parameters = mCamera.getParameters();
//        parameters.setPictureSize(640, 480);
        parameters.setPreviewSize(640, 480);
//        parameters.setPreviewSize(480, 320);
//        parameters.setPreviewSize(320, 240);
//        parameters.setPreviewSize(240, 160 );
//        parameters.setPreviewSize(176, 144 );
        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");

        if (mCamera == null) {
            initCamera();
            mCamera.startPreview();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera == null) {
            return;
        }

        Log.i(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");

        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        Log.i(TAG, "onPreviewFrame");
        Log.i(TAG, "onPreviewFrame Thread:" + Thread.currentThread().getName());

        //每次只处理一个图像，若前面的图像未处理完成，舍弃当前图像，提升性能
        if(mVector.isEmpty()){
            Log.i(TAG, "byte data ADD");
            mVector.add(data);
            new DrawTask().execute();
        }
    }

    public void releaseCamera() {
        mCameraManager.releaseCamera(mCamera);
        mCamera = null;
        mCameraExist = false;
    }

    /**
     * 按正方形裁切图片
     */
    public Bitmap cropImage(Bitmap bitmap) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();

        int wh = w > h ? h : w;// 裁切后所取的正方形区域边长

        int centerX = w / 2;
        int centerY = h / 2;

        //下面这句是关键
        return Bitmap.createBitmap(bitmap, centerX - wh / 2, centerY - wh / 2, wh, wh, null, false);
    }

    public Bitmap scaleImage(Bitmap bitmap, float sx, float sy) {
        Matrix m = new Matrix();
        m.postScale(sx, sy);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private class DrawTask extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            byte[] data =mVector.firstElement();

            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            int cw = mCamera.getParameters().getPreviewSize().width;
            int ch = mCamera.getParameters().getPreviewSize().height;
            int[] rgb = new int[cw * ch];

            long lastTime = System.currentTimeMillis();
//            decodeYUV420SP(rgb, data, cw, ch);
            GPUImageNativeLibrary.YUVtoARBG(data, cw, ch,
                    rgb);
            Log.i(TAG, "decodeYUV420SP time:" + (System.currentTimeMillis() - lastTime));

//            Bitmap bitmap = Bitmap.createBitmap(rgb, 100, 100, Bitmap.Config.ARGB_8888);
            Bitmap bitmap = Bitmap.createBitmap(rgb, cw, ch, Bitmap.Config.ARGB_8888);

            //图片切割成正方形
//            bitmap = cropImage(bitmap);
            //图片缩放
//            bitmap = scaleImage(bitmap, (float) width / bitmap.getWidth(), (float) height / bitmap.getHeight());

            draw(bitmap);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean flag) {
            //移除该元素
            mVector.remove(0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (srcResId != -1) {
            int centerX = canvas.getWidth() / 2;
            int centerY = canvas.getHeight() / 2;

            Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(), srcResId);
            canvas.drawBitmap(srcBitmap, centerX - srcBitmap.getWidth() / 2, centerY - srcBitmap.getHeight() / 2, mPaint);

            recycle(srcBitmap);
        }
    }

    private void draw(Bitmap bitmap) {
        float scaleUp = 3.0f; //缩放比例
        float scaleDown = 1/scaleUp;

//        Canvas canvas = holder.lockCanvas();  // 获取画布
//        if (canvas == null) {
//            return;
//        }

//        int centerX = canvas.getWidth() / 2;
//        int centerY = canvas.getHeight() / 2;

//        canvas.drawRGB(255, 255, 255);

        //开始绘制
//        canvas.save();

//        canvas.rotate(90, centerX, centerY);
        long lastTime = System.currentTimeMillis();

        int originWidth = bitmap.getWidth();
        int originHeight = bitmap.getHeight();

//        bitmap =BitmapUtils.resizeBmp(bitmap,(int)(originWidth/scaleUp),(int)(originHeight/scaleUp));
//        bitmap = BitmapUtils.scale(bitmap,scaleDown);
//        bitmap = FastBlur.doBlur(bitmap, 30, true);
        bitmap = FastBlur.blur(bitmap, 320, 480);
//        bitmap = FastBlur.doBlur(FastBlur.blur(bitmap, 320, 480), 5, true);
//        bitmap =BitmapUtils.resizeBmp(bitmap,originWidth,originHeight);
//        bitmap = BitmapUtils.scale(bitmap,scaleUp);
        Log.i(TAG, "blurCommon time:" + (System.currentTimeMillis() - lastTime));
//        canvas.drawBitmap(bitmap, 0, 0, mPaint);

//        canvas.restore();

//        holder.unlockCanvasAndPost(canvas);  // 解锁画布，提交图像

        recycle(bitmap);
    }

    private void recycle(Bitmap bitmap) {
        MainActivity.handle.sendMessage(MainActivity.handle.obtainMessage(111, bitmap));
//        if (bitmap != null && !bitmap.isRecycled()) {
//            bitmap.recycle();
//        }
    }
}






