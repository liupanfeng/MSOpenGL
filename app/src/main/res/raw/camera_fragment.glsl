/*片元着色器  上色的*/

/*导入 samplerExternalOES */
#extension GL_OES_EGL_image_external : require

/*float 数据的精度 （precision lowp = 低精度） 8位
 （precision mediump = 中精度）   16位
 （precision highp = 高精度）    32 位
 */
precision mediump float;

/*
* 根据上面的数据的精度，写下面的 采样器 相机的数据
* 由于是 安卓的相机，就不能用他  sampler2D == GL_TEXTURE_2D
* uniform sampler2D vTexture;
*
* samplerExternalOES才能采样相机的数据 == GLES11Ext.GL_TEXTURE_EXTERNAL_OES
*/
uniform samplerExternalOES vTexture;
/*
*  把这个最终的计算成果，给片段着色器，拿到最终的成果才能上色
*/
varying vec2 aCoord;

void main() {
    /*
    * texture2D (采样器, 坐标)   opengles 内置函数
    * gl_FragColor OpenGL着色器语言内置的变量
    * vTexture : 是采样器  aCoord是经过处理的纹理坐标
    */
     gl_FragColor = texture2D(vTexture, aCoord);

    // 305911公式：黑白电视效果，其实原理就是提取出Y分量
//    vec4 rgba =texture2D(vTexture, aCoord);
//    float gray = (0.30 * rgba.r   + 0.59 * rgba.g + 0.11* rgba.b); // 其实原理就是提取出Y分量 ,就是黑白电视
//    gl_FragColor = vec4(gray, gray, gray, 1.0);

    /*在网上Copy的公式：底片效果  两次上色，就恢复了*/
//    vec4 rgba = texture2D(vTexture, aCoord);  // rgba
//    gl_FragColor = vec4(1.-rgba.r, 1.-rgba.g, 1.-rgba.b, rgba.a);
}

