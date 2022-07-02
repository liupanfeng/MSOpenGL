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


    private int mWidth;
    private int mHeight;


    /**
     * @param model        OpenCV人脸的模型的文件路径
     * @param seeta        中科院的那个模型（五个关键点的特征点的文件路径）
     * @param cameraHelper 需要把CameraID传递给C++层
     */
    public FaceTrack(String model, String seeta, CameraHelper cameraHelper) {
        Log.d("lpf", "model=" + model);
        Log.d("lpf", "seeta=" + seeta);
        mCameraHelper = cameraHelper;
        /*传入人脸检测模型到C++层处理，返回FaceTrack.cpp的地址指向*/
        self = native_create(model, seeta);
        Log.d("lpf", "native_create success");
        mHandlerThread = new HandlerThread("FaceTrack");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                /*子线程 耗时再久 也不会对其他地方 (如：opengl绘制线程) 产生影响*/
                synchronized (FaceTrack.this) {
                    if (msg.obj == null) {
                        Log.e("lpf", "obj is null");
                        return;
                    }

                    Log.e("lpf", "native_detector start mWidth="+mWidth+" mHeight="+mHeight);
                    mFace = native_detector(self, (byte[]) msg.obj,
                            mCameraHelper.getCameraID(), 1080, 1684);
                    if (mFace != null) {
                        Log.e("拍摄了人脸mFace.toString:", mFace.toString());
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

    /**
     * 这个函数很重要
     *
     * @return 如果能拿到mFace，就证明有人脸最终信息 和 关键点信息
     */
    public Face getFace() {
        return mFace;
    }

    /**
     * @param data NV21 Camera的数据 byte[]
     */
    public void detector(byte[] data, int width, int height) {
        mWidth = width;
        mHeight = height;
        Log.e("lpf","mWidth="+mWidth+" mHeight="+mHeight);
        mHandler.removeMessages(100);
        Message message = mHandler.obtainMessage(100);
        message.obj = data;
        mHandler.sendMessage(message);
    }


    /*------------------------------------native 函数区---------------------------------------*/

    /**
     * 执行人脸探测工作
     *
     * @param self     Face.java对象的地址指向long值
     * @param data     Camera相机 byte[] data NV21摄像头的数据
     * @param cameraId Camera相机ID，前置摄像头，后置摄像头
     * @param width    宽度
     * @param height   高度
     * @return 若Face==null：代表没有人脸信息+人脸特征，  若Face有值：人脸框x/y，+ 特侦点（人脸框x/y + 双眼关键点）
     */
    private native Face native_detector(long self, byte[] data, int cameraId, int width, int height);

    /**
     * 传入人脸检测模型到C++层处理
     *
     * @param model OpenCV人脸模型
     * @param seeta Seeta中科院的人脸关键点模型
     * @return FaceTrack.cpp地址指向long值
     */
    private native long native_create(String model, String seeta);

    /**
     * 开始追踪
     *
     * @param self
     */
    private native void native_start(long self);

    /**
     * 停止追踪
     *
     * @param self
     */
    private native void native_stop(long self);
}
