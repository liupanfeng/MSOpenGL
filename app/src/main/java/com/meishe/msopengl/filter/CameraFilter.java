package com.meishe.msopengl.filter;

import android.content.Context;
import android.opengl.GLES11Ext;

import com.meishe.msopengl.R;
import com.meishe.msopengl.helper.TextureHelper;

import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * @author : lpf
 * @FileName: CameraFilter
 * @Date: 2022/6/17 17:03
 * @Description: 离屏幕渲染FBO
 */
public class CameraFilter extends BaseFrameFilter {

    /**
     * 变换矩阵
     */
    private float[] matrix;


    public CameraFilter(Context context) {
        super(context, R.raw.camera_vertex, R.raw.camera_fragment);
    }

    /**
     * @param textureId 这里的纹理id 是摄像头的
     * @return
     */
    @Override
    public int onDrawFrame(int textureId) {

        glViewport(0, 0, mWidth, mHeight);  // 设置视窗大小

        /*渲染到 FBO离线缓存中*/
        /*绑定FBO缓存（否则会绘制到屏幕上） 我们最终的效果是 离屏渲染*/
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        /*必须要使用着色器程序一次*/
        glUseProgram(mProgramId);

        /*绘制操作 顶点坐标赋值 */
        mVertexBuffer.position(0);
        /*传值*/
        glVertexAttribPointer(vPosition, 2,
                GL_FLOAT, false, 0, mVertexBuffer);
        /*激活*/
        glEnableVertexAttribArray(vPosition);

        /*纹理坐标赋值*/
        mTextureBuffer.position(0);
        /*传值*/
        glVertexAttribPointer(vCoord, 2,
                GL_FLOAT, false, 0, mTextureBuffer);
        /*激活*/
        glEnableVertexAttribArray(vCoord);

        /*变换矩阵，在CameraFilter这里需要处理，后面的BaseFilter就不需要了*/
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);

        /*片元 vTexture  激活图层*/
        glActiveTexture(GL_TEXTURE0);

        /*摄像头打交道采样器：使用额外拓展的，不能使用公用的那个 */
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        glUniform1i(vTexture, 0);
        /*通知 opengl 绘制*/
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        /*FBO的关键在这里做了解绑操作*/
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        /* FBO的纹理ID，返回了*/
        return mFrameBufferTextures[0];
    }

    /**
     * 接收外界传递进来的 变换矩阵
     * @param matrix
     */
    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    @Override
    protected void changeTextureData() {}
}
