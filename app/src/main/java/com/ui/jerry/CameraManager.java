package com.ui.jerry;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Method;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD;


/**
 * 相机管理类 （singleTon）
 *
 * @author jerry
 * @date 2015-09-01
 */
public class CameraManager  implements ICameraHelper{
    private static CameraManager mInstance;
    private final ICameraHelper mCameraHelper;

    private FlashLigthStatus mLightStatus ;
    private CameraDirection mFlashDirection ;

    private TextView m_tvFlashLight,m_tvCameraDireation;  //绑定的闪光灯和前后置镜头切换控件

    public static final int[] RES_DRAWABLE_FLASHLIGHT = {R.drawable.selector_btn_flashlight_off,R.drawable.selector_btn_flashlight_on,R.drawable.selector_btn_flashlight_auto};
    public static final int[] RES_DRAWABLE_CAMERA_DIRECTION ={R.drawable.selector_btn_camera_back,R.drawable.selector_btn_camera_front};

    public static final int[] RES_STRING_FLASHLIGHT = {R.string.topic_camera_flashlight_off,R.string.topic_camera_flashlight_on,R.string.topic_camera_flashlight_auto};
    public static final int[] RES_STRING_CAMERA_DIRECTION = {R.string.topic_camera_back,R.string.topic_camera_front};

    //屏蔽默认构造方法
    private CameraManager(Context context) {
        //todo 读取用户设置
        if(SDK_INT >= GINGERBREAD){
            mCameraHelper = new CameraHelperGBImpl();
        } else {
            mCameraHelper = new CameraHelperBaseImpl(context);
        }

        mLightStatus = FlashLigthStatus.LIGHT_AUTO; //默认 自动
        mFlashDirection = CameraDirection.CAMERA_BACK; //默认后置摄像头
    }

    public static CameraManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (CameraManager.class) {
                if (mInstance == null) {
                    mInstance = new CameraManager(context);
                }
            }
        }
        return mInstance;
    }

    public FlashLigthStatus getLightStatus() {
        return mLightStatus;
    }

    public void setLightStatus(FlashLigthStatus mLightStatus) {
        this.mLightStatus = mLightStatus;

        //todo 保留用户设置

        //
        if(m_tvFlashLight!=null){
            m_tvFlashLight.setText(RES_STRING_FLASHLIGHT[mLightStatus.ordinal()]);
            m_tvFlashLight.setCompoundDrawablesWithIntrinsicBounds(RES_DRAWABLE_FLASHLIGHT[mLightStatus.ordinal()],0,0,0);
        }
    }

    public CameraDirection getCameraDirection() {
        return mFlashDirection;
    }

    public void setCameraDirection(CameraDirection mFlashDirection) {
        this.mFlashDirection = mFlashDirection;

        //todo 保留用户设置
        if(m_tvCameraDireation!=null){
            m_tvCameraDireation.setText(RES_STRING_CAMERA_DIRECTION[mFlashDirection.ordinal()]);
            m_tvCameraDireation.setCompoundDrawablesWithIntrinsicBounds(RES_DRAWABLE_CAMERA_DIRECTION[mFlashDirection.ordinal()],0,0,0);
        }
    }

    @Override
    public int getNumberOfCameras() {
        return mCameraHelper.getNumberOfCameras();
    }

    @Override
    public Camera openCamera(int id) {
        return mCameraHelper.openCamera(id);
    }

    @Override
    public Camera openDefaultCamera() {

        return mCameraHelper.openDefaultCamera();
    }

    @Override
    public Camera openCameraFacing(int facing) {
        return  mCameraHelper.openCameraFacing(facing);
    }

    @Override
    public boolean hasCamera(int facing) {
        return mCameraHelper.hasCamera(facing);
    }

    @Override
    public void getCameraInfo(int cameraId, Camera.CameraInfo cameraInfo) {
        mCameraHelper.getCameraInfo(cameraId,cameraInfo);
    }

    public boolean hasFrontCamera() {
        return hasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    public boolean hasBackCamera() {
        return hasCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    public boolean canSwitch(){
        return  hasFrontCamera()&&hasBackCamera();
    }

    /**
     * 绑定闪光灯、摄像头设置控件
     * @param tvFlashLight
     * @param tvCameraDireation
     */
    public void bindView(TextView tvFlashLight,TextView tvCameraDireation){
        m_tvFlashLight = tvFlashLight;
        m_tvCameraDireation = tvCameraDireation;

        //刷新视图
        setLightStatus(getLightStatus());
        setCameraDirection(getCameraDirection());

        //设置监听
        if(m_tvFlashLight!=null){
            m_tvFlashLight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setLightStatus(getLightStatus().next());
                }
            });
        }


    }

    /**
     * 打开相机界面
     */
    public void openCameraActivity(Context context) {
        Intent intent = new Intent(context, CameraActivity.class);
        context.startActivity(intent);
    }

    //控制图像的正确显示方向
    public void setDispaly( Camera camera) {
        if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setRotation(90);
        }
    }

    //实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation",
                    new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }

    public void releaseCamera(Camera camera){
        if(camera!=null){
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }
    /**
     * 闪光灯状态
     */
    public enum FlashLigthStatus {
        LIGTH_OFF, LIGHT_ON, LIGHT_AUTO;

        //不断循环的枚举
        public FlashLigthStatus next(){
            int index = ordinal();
            int len = FlashLigthStatus.values().length;
            return FlashLigthStatus.values()[(index+1)%len];
        }
    }

    /**
     * 前置还是后置摄像头
     */
    public enum CameraDirection {
         CAMERA_BACK,CAMERA_FRONT;

        //不断循环的枚举
        public CameraDirection next(){
            int index = ordinal();
            int len = CameraDirection.values().length;
            return CameraDirection.values()[(index+1)%len];
        }
    }
}
