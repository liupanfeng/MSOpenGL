//
// Created by 刘静盼 on 2022/6/26.
//

#include "FaceTrack.h"


FaceTrack::FaceTrack(const char *model, const char *seeta) {
    /*OpenCV主探测器*/
    Ptr<CascadeDetectorAdapter> mainDetector=
            makePtr<CascadeDetectorAdapter>(makePtr<CascadeClassifier>(model));
    /*OpenCV跟踪探测器*/
    Ptr<CascadeDetectorAdapter> trackingDetector=
            makePtr<CascadeDetectorAdapter>(makePtr<CascadeClassifier>(seeta));

    DetectionBasedTracker::Parameters detectorParams;
    /*OpenCV创建追踪器，开始跟踪，停止跟踪使用 */
    tracker= makePtr<DetectionBasedTracker>(mainDetector,
                                               trackingDetector,detectorParams);
    /*Seeta人脸关键点*/
    faceAlignment= makePtr<seeta::FaceAlignment>(seeta);

}

/**
 * 拿到数据对OpenGL着色器，对眼睛进行放大操作
 * @param src
 * @param rects
 */
void FaceTrack::detector(Mat src, vector<Rect2f> &rects) {
    vector<Rect> faces;
    /*处理灰度图(OpenCV的东西，灰度，色彩 影响我们人脸追踪)*/
    tracker->process(src);
    /*得到人脸框框的Rect */
    tracker->getObjects(faces);

    /*判断true，说明非零，有人脸*/
    if (faces.size()) {
        Rect face = faces[0];
        /* 然后把跟踪出来的这个人脸，保存到rects里面去*/
        rects.push_back(Rect2f(face.x,face.y,face.width,face.height));

        /*image_data就是图像数据*/
        seeta::ImageData image_data(src.cols, src.rows);
        /* (人脸的信息 要送去检测的) = (把待检测图像)*/
        image_data.data = src.data;

        /*人脸追踪框 信息绑定  人脸关键点定位*/
        seeta::FaceInfo face_info; // 人脸的信息 要送去检测的
        seeta::Rect bbox; // 人脸框框的信息
        bbox.x = face.x;           // 把人脸信息的x 给 face_info
        bbox.y = face.y;           // 把人脸信息的y 给 face_info
        bbox.width = face.width;   // 把人脸信息的width 给 face_info
        bbox.height = face.height; // 把人脸信息的height 给 face_info
        face_info.bbox = bbox;     // 把人脸信息的bbox 给 face_info

        /*特征点的检测，固定了5个点*/
        seeta::FacialLandmark points[5];

        /*执行采集出 五个点*/
        faceAlignment->PointDetectLandmarks(image_data, face_info, points);

        /*把五个点 转换 ，因为第二个参数需要 Rect2f*/
        for (int i = 0; i < 5; ++i) {
            /*不需要宽和高，只需要保存点就够了*/
            rects.push_back(Rect2f(points[i].x, points[i].y, 0, 0));
        }
    }
}

void FaceTrack::startTracking() {
    if (tracker){
        tracker->run();
    }
}

void FaceTrack::stopTracking() {
    if (tracker){
        tracker->stop();
    }
}

