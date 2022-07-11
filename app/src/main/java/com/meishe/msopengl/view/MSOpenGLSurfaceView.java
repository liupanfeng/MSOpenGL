package com.meishe.msopengl.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.meishe.msopengl.Speed;
import com.meishe.msopengl.render.MSOpenGLRenderer;

/**
 * @author : lpf
 * @FileName: MSOpenGLView
 * @Date: 2022/6/16 13:19
 * @Description:  显示Camera预览的画面（OpenGL的处理）
 *  也可以通过继承SurfaceView来实现，不过需要自己处理EGL部分内容，很复杂
 */
public class MSOpenGLSurfaceView extends GLSurfaceView {

    private MSOpenGLRenderer mMsOpenGLRenderer;

    private Speed mSpeed = Speed.MODE_NORMAL;



    public MSOpenGLSurfaceView(Context context) {
        this(context,null);
    }

    public MSOpenGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        /*
        * 设置EGL版本
        * 2 代表是 OpenGLES 2.0
        * */
        setEGLContextClientVersion(2);

        /*
        * 设置渲染器
        * EGL 开启一个 GLThread.start  run  (必须通过GLThread调用来调用三个函数)
        * { Renderer.onSurfaceCreated ...onSurfaceChanged  onDrawFrame }
        *
        * */
        mMsOpenGLRenderer = new MSOpenGLRenderer(this);

        setRenderer(mMsOpenGLRenderer);

        /*
        * 设置渲染器模式
        * RENDERMODE_WHEN_DIRTY 按需渲染，有帧数据的时候才会去渲染（注意：效率高，需要手动调用一次才行）
        * RENDERMODE_CONTINUOUSLY 每隔16毫秒，读取更新一次（如果没有显示上一帧）
        * */
        /*使用手动模式*/
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        mMsOpenGLRenderer.surfaceDestroyed();
    }


    /**
     * 开始录制
     * 变速录制功能
     */
    public void startRecording() {
        float speed = 1.0f;
        switch (mSpeed){
            case MODE_EXTRA_SLOW:
                speed = 0.3f;
                break;
            case MODE_SLOW:
                speed = 0.5f;
                break;
            case MODE_NORMAL:
                speed = 1.0f;
                break;
            case MODE_FAST:
                speed = 1.5f;
                break;
            case MODE_EXTRA_FAST:
                speed = 3.0f;
                break;
        }
        mMsOpenGLRenderer.startRecording(speed);
    }

    /**
     * 圆形红色按钮的 按住拍 的 录制完成
     */
    public void stopRecording() {
        mMsOpenGLRenderer.stopRecording();
    }


    /**
     * 极慢 慢 标准 快 极快 模式的设置函数
     * @param modeExtraSlow 此枚举就是：极慢 慢 标准 快 极快 选项
     */
    public void setSpeed(Speed modeExtraSlow) {
        mSpeed = modeExtraSlow;
    }

    /**
     * 是否启动大眼功能
     * @param isChecked
     */
    public void enableBigEye(boolean isChecked) {
        mMsOpenGLRenderer.enableBigEye(isChecked);
    }
}
