package com.meishe.msopengl.filter;

import android.content.Context;

import com.meishe.msopengl.helper.TextureHelper;

import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteFramebuffers;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glTexImage2D;

/**
 * @author : lpf
 * @FileName: BaseFrameFilter
 * @Date: 2022/6/28 22:19
 * @Description:
 */
public class BaseFrameFilter extends BaseFilter {
    /**
     * FBO 缓冲区
     */
    protected int[] mFrameBuffers;
    /**
     * FBO 纹理缓冲区
     */
    protected int[] mFrameBufferTextures;

    /**
     * @param context
     * @param vertexSourceId   顶点着色器
     * @param fragmentSourceId 片元着色器
     */
    public BaseFrameFilter(Context context, int vertexSourceId, int fragmentSourceId) {
        super(context, vertexSourceId, fragmentSourceId);
    }

    @Override
    protected void changeTextureData() {
        super.changeTextureData();
        float[] TEXTURE = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
        };
        mTextureBuffer.clear();
        mTextureBuffer.put(TEXTURE);
    }

    @Override
    public void onReady(int width, int height) {
        super.onReady(width, height);
        if (mFrameBuffers != null) {
            releaseFrameBuffers();
        }

        /*创建fbo*/
        mFrameBuffers = new int[1];
        /*
         * int n, fbo个数
         * int[] framebuffers, 保存fbo id的数组
         * int offset 数组中第几个来保存
         * */
        glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);

       /*创建属于fbo的纹理*/
        mFrameBufferTextures = new int[1];
        /*生成并配置纹理*/
        TextureHelper.genTextures(mFrameBufferTextures);

        /*
        * 让fbo与纹理发生关系
        * 生成2d纹理图像
        * 目标 2d纹理 + 等级 + 格式 +宽 + 高 + 边界 + 格式 + 数据类型(byte) + 像素数据
        * */
        glBindTexture(GL_TEXTURE_2D, mFrameBufferTextures[0]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,null);

        /*让fbo与纹理绑定起来*/
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        /*解绑 离开屏幕的关键*/
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * 释放当前FBO的父类
     */
    private void releaseFrameBuffers() {
        /* 删除fbo的纹理*/
        if (mFrameBufferTextures != null) {
            glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }

        /*删除fbo*/
        if (mFrameBuffers != null) {
            glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    @Override
    public void release() {
        super.release();
        releaseFrameBuffers();
    }
}
