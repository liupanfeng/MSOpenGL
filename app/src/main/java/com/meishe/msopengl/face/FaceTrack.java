package com.meishe.msopengl.face;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.meishe.msopengl.helper.CameraHelper;

/**
 * @author : lpf
 * @FileName: FaceTrack
 * @Date: 2022/6/26 18:05
 * @Description:
 */
public class FaceTrack {
    /**
     * 手机相机预览工具类
     */
    private CameraHelper mCameraHelper;
    /**
     * 此Handler方便开启一个线程
     */
    private Handler mHandler;
    /**
     * 此HandlerThread方便开启一个线程
     */
    private HandlerThread mHandlerThread;
    /**
     * FaceTrack.cpp对象的地址指向long值
     */
    private long self;
    /**
     * 最终人脸跟踪的结果
     */
    private Face mFace;

    /**
     * @param model        OpenCV人脸的模型的文件路径
     * @param seeta        中科院的那个模型（五个关键点的特征点的文件路径）
     * @param cameraHelper 需要把CameraID传递给C++层
     */
    public FaceTrack(String model, String seeta, CameraHelper cameraHelper) {
        mCameraHelper = cameraHelper;
        self = native_create(model, seeta); // 传入人脸检测模型到C++层处理，返回FaceTrack.cpp的地址指向

        mHandlerThread = new HandlerThread("FaceTrack");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                /*子线程 耗时再久 也不会对其他地方 (如：opengl绘制线程) 产生影响*/
                synchronized (FaceTrack.this) {
                 mFace = native_detector(self, (byte[]) msg.obj,
                         mCameraHelper.getCameraID(),800, 480);
                 if (mFace != null) {
                       Log.e("拍摄了人脸mFace.toString:", mFace.toString()); // 看看打印效果
                 }
                }
            }
        };
    }

    /**
     * 启动跟踪器 OpenCV
     */
    public void startTrack() {
        native_start(self);
    }



    /**
     * 停止跟踪器 OpenCV
     */
    public void stopTrack() {
        synchronized (this) {
            mHandlerThread.quitSafely();
            mHandler.removeCallbacksAndMessages(null);
            native_stop(self);
            self = 0;
        }
    }






    /*------------------------------------native 函数区---------------------------------------*/

    private native Face native_detector(long self, byte[] obj, int cameraID, int i, int i1) ;

    private native long native_create(String model, String seeta);

    private native void native_start(long self);

    private native void native_stop(long self);
}
