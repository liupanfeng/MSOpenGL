package com.meishe.msopengl.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.IOException;

/**
 * @author : lpf
 * @FileName: MSMediaRecorder
 * @Date: 2022/6/26 22:01
 * @Description: 录制视频的工具类 使用MediaCodec实现的
 */
public class MSMediaRecorder {

    /**
     * 宽度
     */
    private final int mWidth;
    /**
     * 高度
     */
    private final int mHeight;
    /**
     * 输出的路径
     */
    private final String mOutputPath;
    /**
     * EGL的上下文
     */
    private final EGLContext mEglContext;
    /**
     * 传递过来的上下文
     */
    private final Context mContext;

    /**
     * 音视频的编解码器
     */
    private MediaCodec mMediaCodec;
    /**
     * MediaCodec的输入Surface画布
     */
    private Surface mInputSurface;
    /**
     * 封装器，视频合成器
     */
    private MediaMuxer mMediaMuxer;
    /**
     * 定义Handler 为了切换到主loop
     */
    private Handler mHandler;
    /**
     * ELG中间件
     */
    private MSEGL mEGL;

    private boolean isStart;
    private int index;
    private float mSpeed;
    private long lastTimeUs;


    public MSMediaRecorder(int width, int height, String outputPath, EGLContext eglContext, Context context) {
        mWidth = width;
        mHeight = height;
        mOutputPath = outputPath;
        mEglContext = eglContext;
        mContext = context;
    }


    public void encodeFrame(final int textureId, final long timestamp) {
        if (!isStart) {
            return;
        }

        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    /*画到虚拟屏幕上*/
                    if (null != mEGL) {
                        mEGL.draw(textureId, timestamp);
                    }
                    /*从编码器中去除数据编码 封装成.mp4文件*/
                    getEncodedData(false);
                }
            });
        }
    }


    public void start(float speed) throws IOException {
        mSpeed = speed;
        /**
         * 创建 MediaCodec 编码器
         * type: 哪种类型的视频编码器
         * MIMETYPE_VIDEO_AVC： H.264
         */
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);

        /**
         * 配置编码器参数
         * 视频格式
         */
        MediaFormat videoFormat = MediaFormat.
                createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        /*设置码率*/
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        /*帧率*/
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        /* 颜色格式 （从Surface中自适应）*/
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        /*关键帧间隔*/
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20);

        /*配置编码器*/
        mMediaCodec.configure(videoFormat,
                null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        /*创建输入 Surface（虚拟屏幕）*/
        mInputSurface = mMediaCodec.createInputSurface();
        /*创建封装器*/
        mMediaMuxer = new MediaMuxer(mOutputPath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        /*配置 EGL 环境*/
        HandlerThread handlerThread = new HandlerThread("MSMediaRecorder");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        mHandler = new Handler(looper);
        mHandler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void run() {
                /* 在一个新的Thread中，初始化EGL环境*/
                mEGL = new MSEGL(mEglContext, mInputSurface, mContext, mWidth, mHeight);
                mMediaCodec.start(); // 启动编码器
                isStart = true;
            }
        });

    }

    public void stop() {
        isStart = false;
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    /*true代表：结束工作*/
                    getEncodedData(true);
                    if (mMediaCodec != null) { // MediaCodec 释放掉
                        mMediaCodec.stop();
                        mMediaCodec.release();
                        mMediaCodec = null;
                    }

                    if (mMediaMuxer != null) { // 封装器 释放掉
                        try {
                            mMediaMuxer.stop();
                            mMediaMuxer.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mMediaMuxer = null;
                    }
                    /* MediaCodec的输入画布/虚拟屏幕 释放掉*/
                    if (mInputSurface != null) {
                        mInputSurface.release();
                        mInputSurface = null;
                    }

                    mEGL.release(); // EGL中间件 释放掉
                    mEGL = null;
                    mHandler.getLooper().quitSafely();
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler = null;
                }
            });
        }
    }

    private void getEncodedData(boolean endOfStream) {
        if (endOfStream) {
            /*让MediaCodec的输入流结束*/
            mMediaCodec.signalEndOfInputStream();
        }
        /*MediaCodec的输出缓冲区*/
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            /*使输出缓冲区出列，最多阻止“timeoutUs”微秒。*/
            int status = mMediaCodec.
                    dequeueOutputBuffer(bufferInfo, 10_000);

        }

    }

}
