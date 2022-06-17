package com.meishe.msopengl.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.meishe.msopengl.render.MSOpenGLRenderer;

/**
 * @author : lpf
 * @FileName: MSOpenGLView
 * @Date: 2022/6/16 13:19
 * @Description:  显示Camera预览的画面（OpenGL的处理）
 *  也可以通过继承SurfaceView来实现，不过需要自己处理EGL部分内容，很复杂
 */
public class MSOpenGLView extends GLSurfaceView {

    public MSOpenGLView(Context context) {
        this(context,null);
    }

    public MSOpenGLView(Context context, AttributeSet attrs) {
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
        setRenderer(new MSOpenGLRenderer(this));

        /*
        * 设置渲染器模式
        * RENDERMODE_WHEN_DIRTY 按需渲染，有帧数据的时候才会去渲染（注意：效率高，需要手动调用一次才行）
        * RENDERMODE_CONTINUOUSLY 每隔16毫秒，读取更新一次（如果没有显示上一帧）
        * */
        /*使用手动模式*/
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
}
