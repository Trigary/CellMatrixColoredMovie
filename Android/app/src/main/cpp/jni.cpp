#include <jni.h>
#include "Processor.h"
#include "Options.h"

using namespace cmcm;

//This file contains all methods that are declared in Java code are implemented here.
//The image processing logic is the same for both the native and the Java implementation,
//therefore the native implementation is not documented for the most part.

extern "C" {

JNIEXPORT jlong JNICALL
Java_hu_trigary_cmcm_app_receive_processor_FrameProcessorThreadNative_init(JNIEnv* env, jclass clazz, jobject manager,
        jint inputHeight, jint inputWidth, jint notRotatedHeight, jint notRotatedWidth, jint rotatedHeight, jint rotatedWidth) {
	Processor* processor = new Processor(env, manager, (int)inputHeight, (int)inputWidth, (int)notRotatedHeight,
										 (int)notRotatedWidth, (int)rotatedHeight, (int)rotatedWidth);
	return (long) processor;
}

JNIEXPORT jlong JNICALL
Java_hu_trigary_cmcm_app_receive_processor_FrameProcessorThreadNative_getOutputMatAddress(JNIEnv* env, jclass clazz, jlong handle) {
	Processor* processor = (Processor*)handle;
	return processor->getOutputMatAddress();
}

JNIEXPORT void JNICALL
Java_hu_trigary_cmcm_app_receive_processor_FrameProcessorThreadNative_process(JNIEnv* env, jclass clazz, jlong handle, jbyteArray buffer) {
	Processor* processor = (Processor*)handle;
	processor->process(env, buffer);
}

JNIEXPORT void JNICALL
Java_hu_trigary_cmcm_app_receive_processor_FrameProcessorThreadNative_release(JNIEnv* env, jclass clazz, jlong handle) {
	Processor* processor = (Processor*)handle;
	processor->release(env);
	delete processor;
}

}
