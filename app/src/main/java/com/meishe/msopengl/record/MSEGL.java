package com.meishe.msopengl.record;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Build;
import android.view.Surface;

import com.meishe.msopengl.filter.MSScreenFilter;

import static android.opengl.EGL14.*;

/**
 * @author : lpf
 * @FileName: MSEGL
 * @Date: 2022/6/26 22:01
 * @Description: 管理EGL环境
 */
public class MSEGL {

    /**
     * EGL显示链接
     */
    private EGLDisplay mEGLDisplay;
    /**
     * EGL最终选择配置的成果
     */
    private EGLConfig mEGLConfig;
    /**
     * EGL 上下文
     */
    private EGLContext mEGLContext;
    /**
     * EGL 独有画布
     */
    private EGLSurface mEGLSurface;
    /**
     * 使用的过滤器  这里选择的基础过滤器
     */
    private MSScreenFilter mMSScreenFilter;

    public MSEGL(EGLContext eglContext, Surface surface,
                 Context context, int width, int height) {
        /*创建EGL环境*/
        createEGL(eglContext);
        /*创建窗口（画布），绘制线程中的图像，直接往这里创建的eglContext上面画*/
        int[] attrib_list = {EGL_NONE };
        mEGLSurface = eglCreateWindowSurface(
                /*EGL显示链接*/
                mEGLDisplay,
                /*EGL最终选择配置的成果*/
                mEGLConfig,
                /*MediaCodec的输入Surface画布*/
                surface,
                /*无任何配置，但也必须要传递 结尾符，否则人家没法玩*/
                attrib_list,
                /*attrib_list的零下标开始读取*/
                0
        ); /* 关联的关键操作，关联（EGL显示链接）（EGL配置）（MediaCodec的输入Surface画布）*/

        /*让 画布 盖住屏幕( 让 mEGLDisplay(EGL显示链接) 和 mEGLSurface(EGL的独有画布) 发生绑定关系)*/
        if(!eglMakeCurrent(mEGLDisplay, // EGL显示链接
                mEGLSurface, // EGL的独有画布 用来画
                mEGLSurface, // EGL的独有画布 用来读
                mEGLContext  // EGL的上下文
        )){
            throw new RuntimeException("eglMakeCurrent fail");
        }

        /*往虚拟屏幕上画画*/
        mMSScreenFilter = new MSScreenFilter(context);
        mMSScreenFilter.onReady(width, height);
    }

    /**
     * 创建EGL
     *
     * @param eglContext
     */
    private void createEGL(EGLContext eglContext) {
        /*获取EGL显示设备: EGL_DEFAULT_DISPLAY(代表 默认的设备 手机屏幕)*/
        mEGLDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        /*初始化设备
         *  主版本号，主版本号的位置下标，副版本号，副版本号的位置下标
         * */
        int[] version = new int[2];

        if (!eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize fail");
        }

        /*选择配置*/
        int[] attrib_list = {
                /* key 像素格式 rgba
                *  value 颜色深度都设置为八位
                * */
                EGL_RED_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE, 8,
                EGL_ALPHA_SIZE, 8,
                /*key 指定渲染api类型
                * value EGL 2.0版本号
                * */
                EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                /*一定要有结尾符*/
                EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        if (!eglChooseConfig(
                /*EGL显示链接*/
                mEGLDisplay,
                /*属性列表*/
                attrib_list,
                /*attrib_list 从数组第零个下标开始找*/
                0,
                /*输出的配置选项成果*/
                configs,
                /*configs 从数组第零个下标开始找*/
                0,
                /*配置的数量，只有一个*/
                configs.length,
                /*需要的配置int数组*/
                num_config,
                /*num_config 从数组第零个下标开始找*/
                0
        )) {
            throw new RuntimeException("eglChooseConfig fail");
        }
        /*最终EGL选择配置的成果 保存起来*/
        mEGLConfig = configs[0];
         /*创建上下文*/
        int[] ctx_attrib_list = {
                /*EGL 上下文客户端版本 2.0*/
                EGL_CONTEXT_CLIENT_VERSION, 2,
                /*一定要有结尾符*/
                EGL_NONE
        };
        mEGLContext = eglCreateContext(
                /*EGL显示链接*/
                mEGLDisplay,
                /*EGL最终选择配置的成果*/
                mEGLConfig,
                /* 共享上下文， 绘制线程 GLThread 中 EGL上下文，达到资源共享*/
                eglContext,
                /*传入上面的属性配置项*/
                ctx_attrib_list,
                0);

        if(null == mEGLContext || mEGLContext == EGL_NO_CONTEXT){
            mEGLContext = null;
            throw new RuntimeException("eglCreateContext fail");
        }

    }

    public void draw(int textureId, long timestamp) {
        /*在虚拟屏幕上渲染还要写一次同样的代码 这个是在EGL的专属线程中的*/
        mMSScreenFilter.onDrawFrame(textureId);
        /*刷新时间戳(如果设置不合理，编码时会采取丢帧或降低视频质量方式进行编码)*/
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, timestamp);
        /* 交换缓冲区数据 绘制操作*/
        eglSwapBuffers(mEGLDisplay, mEGLSurface);

    }

    public void release() {
        eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE,EGL_NO_CONTEXT);
        eglDestroySurface(mEGLDisplay, mEGLSurface);
        eglDestroyContext(mEGLDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEGLDisplay);
    }
}
