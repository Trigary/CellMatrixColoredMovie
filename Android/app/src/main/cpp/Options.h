#ifndef CMCM_NATIVE_OPTIONS_H
#define CMCM_NATIVE_OPTIONS_H

#include <jni.h>
#include <opencv2/core/types.hpp>

using namespace cv;

namespace cmcm {

class Options {
public:
    jobject manager;
    jmethodID javaMethod;
    Size imageSize;
    double minContourArea;
    int maxCodeSize;
    const Size thresholdBlurSize = Size(3, 3);
    const double approxPolygonalEpsilon = 0.04;
    const int blackScanLength = 13;
    const double blackThreshold = 255 * 0.15;

    inline void initialize(JNIEnv* jniEnv, jobject paramManager, int imageHeight, int imageWidth) {
        manager = jniEnv->NewGlobalRef(paramManager);
        javaMethod = jniEnv->GetMethodID(jniEnv->GetObjectClass(manager), "onContentChunksExtracted", "(I[I)V");
        imageSize = Size(imageWidth, imageHeight);
        minContourArea = imageSize.area() * 0.1;
        maxCodeSize = imageHeight < imageWidth ? imageHeight : imageWidth;
    }

    inline void release(JNIEnv* jniEnv) {
        jniEnv->DeleteGlobalRef(manager);
        manager = jobject();
        javaMethod = jmethodID();
    }

};

}

#endif //CMCM_NATIVE_OPTIONS_H
