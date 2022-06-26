package com.meishe.msopengl.render;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.meishe.msopengl.filter.CameraFilter;
import com.meishe.msopengl.filter.MSScreenFilter;
import com.meishe.msopengl.helper.CameraHelper;
import com.meishe.msopengl.record.MSMediaRecorder;
import com.meishe.msopengl.utils.PathUtils;
import com.meishe.msopengl.view.MSOpenGLView;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

/**
 * @author : lpf
 * @FileName: MSOpenGLRender
 * @Date: 2022/6/16 13:25
 * @Description: openGL 渲染器
 */
public class MSOpenGLRenderer implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    private MSOpenGLView mMSOpenGLView;
    /**
     * 相机预览帮助类
     */
    private CameraHelper mCameraHelper;
    /**
     * 纹理id
     */
    private int[] mTextureID;
    /**
     * 从图像流中捕获帧作为 OpenGL ES 纹理
     */
    private SurfaceTexture mSurfaceTexture;
    /**
     * FBO 离屏渲染 过滤器
     */
    private CameraFilter mCameraFilter;

    /**
     * 过滤器
     */
    private MSScreenFilter mMSScreenFilter;

    /**
     * 矩阵数据，变换矩阵
     */
    float[] mtx = new float[16];

    /**
     * 录制工具
     */
    private MSMediaRecorder mMSMediaRecorder;


    /**
     * 通过构造函数，将GLSurfaceView传递过来
     *
     * @param msOpenGLView
     */
    public MSOpenGLRenderer(MSOpenGLView msOpenGLView) {
        this.mMSOpenGLView = msOpenGLView;
    }


    /**
     * Surface创建时 回调此函数
     *
     * @param gl
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraHelper = new CameraHelper((Activity) mMSOpenGLView.getContext(),
                Camera.CameraInfo.CAMERA_FACING_FRONT, 800, 400);

        /* 获取纹理ID 可以理解成画布*/
        mTextureID = new int[1];
        /*
         *  用来生成纹理 给纹理赋值的
         *   1.长度 只有一个 1
         *   2.纹理ID，是一个数组
         *   3.offset:0 使用数组的0下标
         * */
        GLES20.glGenTextures(mTextureID.length, mTextureID, 0);
        /*实例化纹理对象*/
        mSurfaceTexture = new SurfaceTexture(mTextureID[0]);
        /*绑定可用帧回调监听*/
        mSurfaceTexture.setOnFrameAvailableListener(this);
        /*先进行FBO离屏渲染*/
        mCameraFilter = new CameraFilter(mMSOpenGLView.getContext());
        /*进行预览显示*/
        mMSScreenFilter = new MSScreenFilter(mMSOpenGLView.getContext());

        /* 初始化录制工具类*/
        EGLContext eglContext = EGL14.eglGetCurrentContext();
        mMSMediaRecorder = new MSMediaRecorder(480, 800,
                PathUtils.getRecordVideoName(), eglContext,
                mMSOpenGLView.getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraHelper.startPreview(mSurfaceTexture);
        mCameraFilter.onReady(width, height); // 先FBO
        /*传递宽 高给filter*/
        mMSScreenFilter.onReady(width, height);
    }

    /**
     * 绘制一帧图像时 回调此函数
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        /* 每次清空之前的 清理成红色的黑板一样*/
        glClearColor(255, 0, 0, 0);
        /*
         * mask 细节看看此文章：https://blog.csdn.net/z136411501/article/details/83273874
         * GL_COLOR_BUFFER_BIT 颜色缓冲区
         * GL_DEPTH_BUFFER_BIT 深度缓冲区
         * GL_STENCIL_BUFFER_BIT 模型缓冲区
         * */
        glClear(GL_COLOR_BUFFER_BIT);

        /*
         * 绘制摄像头数据
         * 将纹理图像更新为图像流中最新的帧数据 刷新一下
         * */
        mSurfaceTexture.updateTexImage();

        /*画布，矩阵数据*/
        mSurfaceTexture.getTransformMatrix(mtx);

        mCameraFilter.setMatrix(mtx);
        /*摄像头，矩阵，都已经做了*/
        int textureId = mCameraFilter.onDrawFrame(mTextureID[0]);
        /*
        *textureId==最终成果的纹理ID
        *最终直接显示的，他是调用了 BaseFilter的onDrawFrame渲染的（简单的显示就行了）
        * 核心在这里：1：画布==纹理ID，
        *  2：mtx矩阵数据
        * */
        mMSScreenFilter.onDrawFrame(textureId);

        /*录制 对经过渲染之后的纹理进行录制*/
        mMSMediaRecorder.encodeFrame(textureId, mSurfaceTexture.getTimestamp());
    }

    /**
     * 有可用的数据时回调此函数，比自动回调的效率高  也可以自动16.6ms回调
     * 需要手动调用一次才行
     * setRenderMode(RENDERMODE_WHEN_DIRTY); 配合用
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mMSOpenGLView.requestRender();
    }


    /**
     * 开始录制
     * @param speed
     */
    public void startRecording(float speed) {
        Log.e("MyGLRender", "startRecording speed:" + speed);
        try {
            mMSMediaRecorder.start(speed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        Log.e("MyGLRender", "stopRecording");
        mMSMediaRecorder.stop();
    }

    /**
     * 进行释放操作
     */
    public void surfaceDestroyed() {
        mCameraHelper.stopPreview();
    }


}
