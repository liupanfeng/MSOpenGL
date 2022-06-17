package com.meishe.msopengl;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES11Ext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * @author : lpf
 * @FileName: MSScreenFilter
 * @Date: 2022/6/16 13:18
 * @Description:  显示到 GLSurfaceView 屏幕
 */
public class MSScreenFilter {

    /*着色器代码*/
    private  String vertexSourceV ;

    /*着色器程序ID*/
    private final int mProgram;

    /* 顶点着色器：位置*/
    private final int vPosition;

    /*顶点着色器：纹理*/
    private final int vCoord;

    /*顶点着色器：矩阵*/
    private final int vMatrix;

    /*片元着色器：采样器 摄像头数据*/
    private final int vTexture;

    /*顶点坐标 nio的buffer缓存*/
    private FloatBuffer mVertexBuffer;
    /* 纹理坐标 nio的buffer缓存*/
    private FloatBuffer mTextureBuffer;
    /*宽度*/
    private int mWidth;
    /* 高度*/
    private int mHeight;

    public MSScreenFilter(Context context) {
        String vertexSource = readTextFileFromResource(context, R.raw.camera_vertex); // 查找到（顶点着色器）的代码字符串
        String fragmentSource = readTextFileFromResource(context, R.raw.camera_fragment); // 查找到（片元着色器）的代码字符串


        /*创建顶点着色器*/
        int vShaderId=glCreateShader(GL_VERTEX_SHADER);
        /*绑定着色器源代码到 着色器（加载着色器的代码）*/
        glShaderSource(vShaderId,vertexSource);
        /*编译着色器代码（编译阶段：编译成功就能拿到顶点着色器ID，编译失败基本上就是着色器代码字符串写错了）*/
        glCompileShader(vShaderId);
        int[] status=new int[1];
        glGetShaderiv(vShaderId,GL_COMPILE_STATUS,status,0);
        if (status[0]!=GL_TRUE){
            throw new IllegalStateException("顶点着色器配置失败");
        }

        /*配置片元着色器*/

        /*创建片元着色器*/
        int fShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        /*绑定着色器源代码到 着色器（加载着色器的代码）*/
        glShaderSource(fShaderId,fragmentSource);
        /*编译着色器代码（编译阶段：编译成功就能拿到顶点着色器ID，编译失败基本上就是着色器代码字符串写错了）*/
        glCompileShader(fShaderId);
        glGetShaderiv(fShaderId,GL_COMPILE_STATUS,status,0);
        if (status[0]!=GL_TRUE){
            throw new IllegalStateException("片元着色器创建失败");
        }

        /*配置着色器程序*/

        /*创建一个着色器程序*/
        mProgram = glCreateProgram();

        /*将前面配置的 顶点 和 片元 着色器 附加到新的程序 上*/

        /*顶点附加*/
        glAttachShader(mProgram,vShaderId);
        /*片元附件*/
        glAttachShader(mProgram,fShaderId);
        /* 链接着色器 mProgram着色器程序 */
        glLinkProgram(mProgram);
        glGetShaderiv(mProgram,GL_LINK_STATUS,status,0);
        if (status[0]!=GL_TRUE){
            throw new IllegalStateException("着色器程序连接失败");
        }



        /*释放，删除着色器，因为用不到 顶点着色器/片元着色器了，只需要有着色器程序mProgram即可*/
        glDeleteShader(vShaderId);
        glDeleteShader(fShaderId);

        /*
         * 获取变量的索引值，通过索引来赋值
         * 顶点着色器里面的如下：
         * */

        /*顶点着色器：的索引值*/
        vPosition=glGetAttribLocation(mProgram,"vPosition");
        /* 顶点着色器：纹理坐标，采样器采样图片的坐标 的索引值*/
        vCoord=glGetAttribLocation(mProgram,"vCoord");
        /*顶点着色器：变换矩阵 的索引值*/
        vMatrix=glGetUniformLocation(mProgram,"vMatrix");
        /*片元着色器：采样器*/
        vTexture=glGetUniformLocation(mProgram,"vTexture");


        /*
         * NIO Buffer（就是一个缓存，存和取） 着色器语言绑定
         * 顶点坐标缓存（顶点：位置 排版） -- vPosition
         * */

        /*分配内存 坐标个数 * xy坐标数据类型 * float占几字节*/
        mVertexBuffer =ByteBuffer.allocateDirect(4*2*4)
                /*使用本地字节序，例如：大端模式，小端模式，这里设置为：跟随OpenGL的变化二变化*/
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.clear();

        /* OpenGL世界坐标*/
        float[] v={
                -1.0f,-1.0f,
                1.0f,-1.0f,
                -1.0f,  1.0f,
                1.0f,1.0f
        };
        mVertexBuffer.put(v);

        /*
         * 纹理坐标缓存（纹理：上色 成果） -- vCoord   === 和屏幕挂钩
         * 分配内存 坐标个数 * xy坐标数据类型 * float占几字节
         * */

        mTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.clear();

        /*屏幕坐标系*/
        /*旋转 180度 */
        float[] t = {
                1.0f, 0.0f,
                0.0f,  0.0f,
                1.0f,  1.0f,
                0.0f,  1.0f,
        };

        mTextureBuffer.put(t);

    }


    public void onReady(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * 绘制操作
     * @param mTextureID 画布 纹理ID
     * @param mtx 矩阵数据
     */
    public void onDrawFrame(int mTextureID, float[] mtx) {

        /*设置视窗大小，从0开始，这个是合理的*/
        glViewport(0,0,mWidth,mHeight);
        /*执行着色器程序*/
        glUseProgram(mProgram);

        /*顶点坐标赋值  NIO的Buffer 用它就要归零 */
        mVertexBuffer.position(0);

        /**
         * 传值（把float[]值传递给顶点着色器）把mVertexBuffer传递到vPosition == size:每次两个xy， stride:0 不跳步
         * 1.着色器代码里面的 标记变量 attribute vec4 mPosition;
         * 2.xy 所以是两个
         * 3.不用管
         * 4.跳步 0 不跳步
         */

        glVertexAttribPointer(vPosition,2,GL_FLOAT,
                false,0,mVertexBuffer);
        /*激活*/
        glEnableVertexAttribArray(vPosition);

        /*纹理坐标赋值*/
        mTextureBuffer.position(0);
        /*传值（把float[]值传递给纹理）
         *把mTexturBuffer传递到vCoord == size:每次两个xy， stride:不跳步*/
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer);
        /*激活*/
        glEnableVertexAttribArray(vCoord);

        /*变换矩阵 把mtx矩阵数据 传递到 vMatrix*/
        glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

        /*vTexture*/
        glActiveTexture(GL_TEXTURE0);

        /*绑定纹理ID --- glBindTexture(GL_TEXTURE_2D ,textureId);
         *如果在片元着色器中的vTexture，不是samplerExternalOES类型，就可以这样写*/
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        /*传递参数 给 片元着色器：采样器*/
        glUniform1i(vTexture, 0);
        /*通知 opengl 绘制 ，从0开始，共四个点绘制*/
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    }






    /**
     * 用于读取 GLSL文件中着色器代码
     * @param context 上下文
     * @param resourceId 传入raw
     * @return GLSL文件中的代码字符串
     */
    public static String readTextFileFromResource(Context context, int resourceId) {
        StringBuilder body = new StringBuilder();
        try {
            InputStream inputStream = context.getResources().openRawResource(resourceId);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not open resource: " + resourceId, e);
        } catch (Resources.NotFoundException nfe) {
            throw new RuntimeException("Resource not found: " + resourceId, nfe);
        }
        return body.toString();
    }

}
