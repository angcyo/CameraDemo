package com.ui.jerry;

import android.hardware.Camera;

/**
 * CameraHelper的统一接口
 * @author jerry
 * @date 2015-09-01
 */
public interface ICameraHelper {

    int getNumberOfCameras();

    Camera openCamera(int id);

    Camera openDefaultCamera();

    Camera openCameraFacing(int facing);

    boolean hasCamera(int facing);

    void getCameraInfo(int cameraId, Camera.CameraInfo cameraInfo);

}
