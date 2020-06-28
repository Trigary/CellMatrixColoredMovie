#ifndef CMCM_NATIVE_PROCESSOR_H
#define CMCM_NATIVE_PROCESSOR_H

#include <jni.h>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/core/types.hpp>
#include "Options.h"
#include "Color.h"
#include "FeatureExtractor.h"
#include "chunk_extractor.h"

using namespace cv;

namespace cmcm {

class Processor {
private:
    Options options = Options();
	Color pixels[128 * 128];
	jint contentArray[128 * 128 / 4 - 2];
	int yuvMatLength;
	Mat yuvMat;
	Mat rgbMat;
	Mat resizedMat;
	Mat rotatedMat;
	FeatureExtractor featureExtractor;
	Mat extractedRgb = Mat();
	Mat extractedBinary = Mat();
	vector<Mat> allContours = vector<Mat>();
	vector<Mat> filteredContours = vector<Mat>();
	Color templateColors[4]{Color::WHITE, Color::WHITE, Color::WHITE, Color::WHITE};
	int resolution;
	double cellSize;
	int rotateClockwise;
	bool flipVertical;

public:
	inline Processor(JNIEnv* jniEnv, jobject manager, int inputHeight, int inputWidth, int notRotatedHeight,
					 int notRotatedWidth, int rotatedHeight, int rotatedWidth) : featureExtractor(options) {
		options.initialize(jniEnv, manager, rotatedHeight, rotatedWidth);
		yuvMatLength = (int) (inputHeight * 1.5) * inputWidth;
		yuvMat = Mat((int) (inputHeight * 1.5), inputWidth, CV_8UC1);
		rgbMat = Mat(inputHeight, inputWidth, CV_8UC3);
		resizedMat = Mat(notRotatedHeight, notRotatedWidth, CV_8UC3);
		rotatedMat = Mat(rotatedHeight, rotatedWidth, CV_8UC3);
		featureExtractor.initialize();
	}

	inline void release(JNIEnv* jniEnv) {
	    options.release(jniEnv);
	}

	inline jlong getOutputMatAddress() {
		return (jlong) &rotatedMat;
	}

	inline void process(JNIEnv* jniEnv, jbyteArray buffer) {
		jniEnv->GetByteArrayRegion(buffer, 0, yuvMatLength, reinterpret_cast<jbyte*>(yuvMat.data));
		cvtColor(yuvMat, rgbMat, COLOR_YUV2RGB_NV21);
		resize(rgbMat, resizedMat, resizedMat.size(), 0, 0, INTER_NEAREST);
		rotate(resizedMat, rotatedMat, ROTATE_90_CLOCKWISE);

		featureExtractor.findContours(rotatedMat, allContours);
		featureExtractor.filterContours(allContours, filteredContours);
		if (!filteredContours.empty()) {
			handleContours(jniEnv);
			filteredContours.clear();
		}
		allContours.clear();
	}

private:
	void handleContours(JNIEnv* jniEnv) {
		for (int contourIndex = 0; contourIndex < filteredContours.size(); contourIndex++) {
			const Mat& contour = filteredContours.at(contourIndex);
			featureExtractor.warpPerspective(rotatedMat, extractedRgb, contour);

			featureExtractor.toBinary(extractedRgb, extractedBinary);
			int result = tryContour();
			if (result == 1) { //border doesn't match
				drawContours(rotatedMat, filteredContours, contourIndex, Scalar(255, 0, 0), 3);
			} else if (result == 2) { //border matches, template cells don't
				drawContours(rotatedMat, filteredContours, contourIndex, Scalar(0, 255, 0), 3);
			} else { //everything matches
				drawContours(rotatedMat, filteredContours, contourIndex, Scalar(0, 0, 255), 3);
				handleGoodContour(jniEnv);
			}
		}
	}

	int tryContour() {
		resolution = featureExtractor.getResolution(extractedBinary);
		cellSize = featureExtractor.getCellSize(resolution);
		if (featureExtractor.getBorderColor(extractedBinary, cellSize) > options.blackThreshold) {
			return 1;
		}

		if (!featureExtractor.getCornerTemplateColors(extractedRgb, cellSize, resolution, templateColors)) {
			return 2;
		}

		rotateClockwise = 3;
		if (templateColors[0] == Color::WHITE) {
			rotateClockwise = 0;
		} else if (templateColors[1] == Color::WHITE) {
			rotateClockwise = 1;
		} else if (templateColors[2] == Color::WHITE) {
			rotateClockwise = 2;
		}

		flipVertical = false;
		if (!(templateColors[(rotateClockwise + 1) % 4] == Color::AQUA)) {
			rotateClockwise += rotateClockwise == 0 || rotateClockwise == 2 ? 1 : -1;
			flipVertical = true;
		}

		//TODO check rest of template cells
		return 0;
	}

	void handleGoodContour(JNIEnv* jniEnv) {
		int max = resolution - 1;
		for (int x = 0; x < resolution; x++) {
			for (int y = 0; y < resolution; y++) {
				int realX = x;
				int realY = flipVertical ? max - y : y;

				//noinspection StatementWithEmptyBody
				if (rotateClockwise == 0) {
					//most probable
				} else if (rotateClockwise == 1) {
					int temp = realX;
					realX = max - realY;
					realY = temp;
				} else if (rotateClockwise == 2) {
					realX = max - realX;
					realY = max - realY;
				} else if (rotateClockwise == 3) {
					int temp = realY;
					realY = max - realX;
					realX = temp;
				}

				pixels[realX + realY * resolution] = Color::getClosest(color_extractor::getCell(extractedRgb, cellSize, x + 1, y + 1));
			}
		}

		extractChunks(pixels, contentArray, resolution);
		int length = resolution * resolution / 4 - 2;
		jintArray jArray = jniEnv->NewIntArray(length);
		jniEnv->SetIntArrayRegion(jArray, 0, length, contentArray);
		jniEnv->CallVoidMethod(options.manager, options.javaMethod, resolution, jArray);
	}
};

}

#endif //CMCM_NATIVE_PROCESSOR_H
