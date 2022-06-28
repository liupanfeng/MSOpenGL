package com.meishe.msopengl.render;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.meishe.msopengl.face.FaceTrack;
import com.meishe.msopengl.filter.BigEyeFilter;
import com.meishe.msopengl.filter.CameraFilter;
import com.meishe.msopengl.filter.MSScreenFilter;
import com.meishe.msopengl.helper.CameraHelper;
import com.meishe.msopengl.record.MSMediaRecorder;
import com.meishe.msopengl.utils.FileUtil;
import com.meishe.msopengl.utils.PathUtils;
import com.meishe.msopengl.view.MSOpenGLSurfaceView;

import java.io.File;

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
    /**
     * 人脸模型名称
     */
    private static final String FACE_MODEL_NAME="lbpcascade_frontalface.xml";
    /**
     * 点位模型名称
     */
    private static final String FACE_POINT_MODEL_NAME="seeta_fa_v1.1.bin";


    private MSOpenGLSurfaceView mMSOpenGLView;
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

    private int mWidth;
    private int mHeight;



    /**
     * 大眼过滤器
     */
    private BigEyeFilter mBigEyeFilter;
    /**
     * 人脸通道，包含人脸信息
     */
    private FaceTrack mFaceTrack;

    /**
     * 通过构造函数，将GLSurfaceView传递过来
     *
     * @param msOpenGLView
     */
    public MSOpenGLRenderer(MSOpenGLSurfaceView msOpenGLView) {
        this.mMSOpenGLView = msOpenGLView;

        /*OpenCV的模型*/
        FileUtil.copyAssets2SDCard(mMSOpenGLView.getContext(), FACE_MODEL_NAME,
                PathUtils.getModelDir()+ File.separator+FACE_MODEL_NAME);

        /* 中科院的模型*/
        FileUtil.copyAssets2SDCard(mMSOpenGLView.getContext(), FACE_POINT_MODEL_NAME,
                PathUtils.getModelDir()+ File.separator+FACE_POINT_MODEL_NAME);
    }


    /**
     * Surface创建时 回调此函数
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
         *  长度 只有一个 1
         *   纹理ID，是一个数组
         *   offset:0 使用数组的0下标
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
        String recordVideoName = PathUtils.getRecordVideoName();
        Log.d("lpf","recordVideoName="+recordVideoName);
        mMSMediaRecorder = new MSMediaRecorder(480, 800,
                recordVideoName, eglContext,
                mMSOpenGLView.getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        mWidth = width;
        mHeight = height;


        /*创建人脸检测跟踪器*/
        mFaceTrack = new FaceTrack(PathUtils.getModelDir()+ File.separator+FACE_MODEL_NAME,
                PathUtils.getModelDir()+File.separator+FACE_POINT_MODEL_NAME,
                mCameraHelper);
        /*启动跟踪器*/
        mFaceTrack.startTrack();


        mCameraHelper.startPreview(mSurfaceTexture);
        /* 先FBO*/
        mCameraFilter.onReady(width, height);
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
        * 绘制大眼
        * */
        if (null != mBigEyeFilter) {
            mBigEyeFilter.setFace(mFaceTrack.getFace());
            textureId = mBigEyeFilter.onDrawFrame(textureId);
        }

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

    public void surfaceDestroyed() {
        mCameraHelper.stopPreview();
        /*停止跟踪器*/
        mFaceTrack.stopTrack();
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



    public void enableBigEye(boolean isChecked) {
        mMSOpenGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (isChecked){
                    mBigEyeFilter = new BigEyeFilter(mMSOpenGLView.getContext());
                    mBigEyeFilter.onReady(mWidth, mHeight);
                }else{
                    mBigEyeFilter.release();
                    mBigEyeFilter = null;
                }
            }
        });
    }
}
