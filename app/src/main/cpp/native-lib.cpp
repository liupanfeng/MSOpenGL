#include <jni.h>
#include <string>

#include <opencv2/imgproc/types_c.h>
#include "FaceTrack.h"

#include "log_util.h"

/*point_detector 人脸关键点模型 关联起来*/



extern "C" JNIEXPORT jstring JNICALL
Java_com_meishe_msopengl_CaptureActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_meishe_msopengl_face_FaceTrack_native_1create(JNIEnv *env, jobject thiz, jstring model,
                                                       jstring seeta) {
    const char *model_ = env->GetStringUTFChars(model, JNI_OK);
    const char *seeta_ = env->GetStringUTFChars(seeta, JNI_OK);

    FaceTrack *faceTrack = new FaceTrack(model_, seeta_);
    loge("create success");
    env->ReleaseStringUTFChars(model, model_);
    env->ReleaseStringUTFChars(seeta, seeta_);

    return reinterpret_cast<jlong>(faceTrack);
}

/**
 * 开始跟踪  OpenCV开启追踪器
 */
extern "C"
JNIEXPORT jobject JNICALL
Java_com_meishe_msopengl_face_FaceTrack_native_1detector(JNIEnv *env, jobject thiz, jlong self,
                                                         jbyteArray data_, jint camera_id,
                                                         jint width,
                                                         jint height) {
    if (self == 0) {
        return NULL;
    }
    jbyte *data = env->GetByteArrayElements(data_, 0);
    /*通过地址反转CPP对象*/
    FaceTrack *faceTrack = reinterpret_cast<FaceTrack *>(self);
    /*OpenCV旋转数据操作  摄像头数据data 转成 OpenCv的 Mat*/
    Mat src(height + height / 2, width, CV_8UC1, data); // 摄像头数据data 转成 OpenCv的 Mat

    /*做调试的时候用的（方便查看：有没有摆正，有没有灰度化 等）*/
    imwrite("/sdcard/camera.jpg", src);
//    loge("把YUV转成RGBA start");
    /*把YUV转成RGBA*/
    cvtColor(src, src, CV_YUV2RGBA_NV21);
//    loge("把YUV转成RGBA success");
    if (camera_id == 1) {
        /*前摄*/
        /*逆时针90度*/
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        /*y 轴 翻转（镜像操作）*/
        flip(src, src, 1);
    } else {
        /*后摄*/
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }
//    loge("OpenCV基础操作  灰度化 start");
    Mat gray;
    /* OpenCV基础操作  灰度化*/
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    /*均衡化处理（直方图均衡化，增强对比效果）*/
    equalizeHist(gray, gray);
    vector<Rect2f> rects;
//    loge("detector start");
    imwrite("/sdcard/detector.jpg", gray);
    /*送去定位，要去做人脸的检测跟踪了*/
//    faceTrack->detector(src, rects);


    ///////////////////////////////////////////////////

    faceTrack->tracker->process(gray);

    std::vector<Rect> faces;
    faceTrack->tracker->getObjects(faces);
    loge("faces size is --------------------%d",faces.size());
    for (Rect face:faces) {
        rectangle(src, face, Scalar(255, 0, 255));
    }

































    ///////////////////////////////////////////////////////





























    env->ReleaseByteArrayElements(data_, data, 0);

    /*他已经有丰富的人脸框框的信息，接下来就是，关键点定位封装操作Face.java*/

    /*上面的代码执行完成后，就拿到了 人脸检测的成果 放置在rects中*/

    /*C++ 反射 实例化 Face.java 并且保证 Face.java有值*/

    /*构建 Face.java的 int imgWidth; 送去检测图片的宽*/
    int imgWidth = src.cols;
    /*构建 Face.java的 int imgHeight; 送去检测图片的高*/
    int imgHeight = src.rows;
    /*如果有一个人脸，那么size肯定大于0*/
    int ret = rects.size();

    loge("imgWidth=%d imgHeight=%d  ret=%d",imgWidth,imgHeight,ret);
    if (ret) {
        /*有人脸信息*/
        jclass clazz = env->FindClass("com/meishe/msopengl/face/Face");
        jmethodID construct = env->GetMethodID(clazz, "<init>", "(IIII[F)V");
        /*int width, int height,int imgWidth,int imgHeight, float[] landmark*/
        /*乘以2是因为，有x与y， 其实size===2，因为rects就一个人脸*/
        int size = ret * 2;

        /*构建 Face.java的 float[] landmarks;*/
        jfloatArray floatArray = env->NewFloatArray(size);
        /*前两个就是人脸的x与y*/
        for (int i = 0, j = 0; i < size; ++j) {
            float f[2] = {rects[j].x, rects[j].y};
            env->SetFloatArrayRegion(floatArray, i, 2, f);
            i += 2;
        }

        Rect2f faceRect = rects[0];
        /*构建 Face.java的 int width; 保存人脸的宽*/
        int faceWidth = faceRect.width;
        /*构建 Face.java的 int height; 保存人脸的高*/
        int faceHeight = faceRect.height;

        /*实例化Face.java对象，*/
        jobject face = env->NewObject(clazz, construct,
                                      faceWidth, faceHeight, imgWidth, imgHeight, floatArray);
        /*rectangle*/
        rectangle(src, faceRect, Scalar(0, 0, 255));
        for (int i = 1; i < ret; ++i) {
            circle(src, Point2f(rects[i].x, rects[i].y), 5, Scalar(0, 255, 0));
        }
        /*做调试的时候用的（方便查看：有没有摆正，有没有灰度化 等）*/
//        imwrite("/sdcard/src.jpg", src);
        /*返回 jobject == Face.java（已经有值了，有人脸所有的信息了，那么就可以开心，放大眼睛）*/
        return face;
    }
    src.release(); // Mat释放工作
    return NULL;

}



extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msopengl_face_FaceTrack_native_1start(JNIEnv *env, jobject thiz, jlong self) {
    if (self) {
        FaceTrack *faceTrack = reinterpret_cast<FaceTrack *>(self);
        faceTrack->startTracking();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_meishe_msopengl_face_FaceTrack_native_1stop(JNIEnv *env, jobject thiz, jlong self) {
    if (self) {
        FaceTrack *faceTrack = reinterpret_cast<FaceTrack *>(self);
        faceTrack->stopTracking();
    }
}