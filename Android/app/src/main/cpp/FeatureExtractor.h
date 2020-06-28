#ifndef CMCM_NATIVE_FEATUREEXTRACTOR_H
#define CMCM_NATIVE_FEATUREEXTRACTOR_H

#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/core/types.hpp>
#include "Color.h"
#include "Options.h"
#include "color_extractor.h"

using namespace cv;
using namespace std;

namespace cmcm {

class FeatureExtractor {
private:
	constexpr static const int ALL_RESOLUTIONS[11]{8, 12, 16, 24, 32, 40, 50, 64, 80, 100, 128};
	static int ALL_CORNER_COLORS;
	Options& options;
	Mat contoursMat;
	Point2f warpDestination[4];
	Point2f contourCorners[4];
	Size warpSize;

public:
    inline FeatureExtractor(Options& paramOptions) : options(paramOptions) {
    }

	inline void initialize() {
		contoursMat = Mat(options.imageSize, CV_8UC1);
		int size = options.maxCodeSize;
		warpDestination[0] = Point(0, 0);
		warpDestination[1] = Point(size, 0);
		warpDestination[2] = Point(0, size);
		warpDestination[3] = Point(size, size);
		warpSize = Size(size, size);
	}

	inline void toBinary(const Mat& rgbInput, Mat& binaryOutput) {
		cvtColor(rgbInput, binaryOutput, COLOR_RGB2GRAY);
		GaussianBlur(binaryOutput, binaryOutput, options.thresholdBlurSize, 0);
		threshold(binaryOutput, binaryOutput, 0, 255, THRESH_OTSU);
	}

	inline void findContours(const Mat& rgbInput, vector<Mat>& output) {
		toBinary(rgbInput, contoursMat);
		Mat hierarchy = Mat();
		cv::findContours(contoursMat, output, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE);
		//TODO if I don't do the chain approximation here,
		//I MIGHT (untested) get a boost in performance and approxPolyDP can take care of that
	}

	inline void filterContours(const vector<Mat>& input, vector<Mat>& output) {
		for (const Mat& contour : input) {
			if (contour.total() < 4 || contourArea(contour) < options.minContourArea) {
				continue;
			}

			Mat newContour = Mat();
			approxPolyDP(contour, newContour, options.approxPolygonalEpsilon * contour.total(), true);
			if (newContour.total() != 4 || !isContourConvex(newContour)) {
				continue;
			}

			//TODO do some other stuff? Check AruCo source
			// https://github.com/opencv/opencv_contrib/blob/master/modules/aruco/src/aruco.cpp#L130

			output.push_back(newContour);
		}
	}

	inline void warpPerspective(const Mat& input, Mat& output, const Mat& contour) {
		contourCorners[0] = contour.at<Point>(0);
		contourCorners[1] = contour.at<Point>(1);
		contourCorners[2] = contour.at<Point>(2);
		contourCorners[3] = contour.at<Point>(3);

		if (contourCorners[0].y > contourCorners[1].y) {
			Point temp = contourCorners[0];
			contourCorners[0] = contourCorners[1];
			contourCorners[1] = temp;
		}
		if (contourCorners[2].y > contourCorners[3].y) {
			Point temp = contourCorners[2];
			contourCorners[2] = contourCorners[3];
			contourCorners[3] = temp;
		}
		if (contourCorners[1].y > contourCorners[2].y) {
			Point temp = contourCorners[1];
			contourCorners[1] = contourCorners[2];
			contourCorners[2] = temp;

			if (contourCorners[0].y > contourCorners[1].y) {
				temp = contourCorners[0];
				contourCorners[0] = contourCorners[1];
				contourCorners[1] = temp;
			}
			if (contourCorners[2].y > contourCorners[3].y) {
				temp = contourCorners[2];
				contourCorners[2] = contourCorners[3];
				contourCorners[3] = temp;

				if (contourCorners[1].y > contourCorners[2].y) {
					temp = contourCorners[1];
					contourCorners[1] = contourCorners[2];
					contourCorners[2] = temp;
				}
			}
		}

		if (contourCorners[0].x > contourCorners[1].x) {
			Point temp = contourCorners[0];
			contourCorners[0] = contourCorners[1];
			contourCorners[1] = temp;
		}
		if (contourCorners[2].x > contourCorners[3].x) {
			Point temp = contourCorners[2];
			contourCorners[2] = contourCorners[3];
			contourCorners[3] = temp;
		}

		Mat perspective = getPerspectiveTransform(contourCorners, warpDestination);
		cv::warpPerspective(input, output, perspective, warpSize, INTER_NEAREST);
	}

	inline int getResolution(const Mat& binaryInput) {
		int pixelCount = options.maxCodeSize;
		int skipPixels = pixelCount / 130;
		int maxSteps = (pixelCount / 10) - skipPixels;

		double rawCellSize = 0;
		int farEnd = pixelCount - skipPixels - 1;
		int offset = options.blackScanLength - 1;
		rawCellSize += 1.0 / findFirstNonBlack(binaryInput, maxSteps,
											   skipPixels, skipPixels, 1, 1, 0, 1); //top left
		rawCellSize += 1.0 / findFirstNonBlack(binaryInput, maxSteps,
											   skipPixels, farEnd, 1, -1, 0, -1 - offset); //bottom left
		rawCellSize += 1.0 / findFirstNonBlack(binaryInput, maxSteps,
											   farEnd, skipPixels, -1, 1, -offset, 1); //top right
		rawCellSize += 1.0 / findFirstNonBlack(binaryInput, maxSteps,
											   farEnd, farEnd, -1, -1, -offset, -1 - offset); //bottom right
		rawCellSize = skipPixels + (1 / (rawCellSize / 4));

		double bestDistance = numeric_limits<double>::max();
		int bestResolution = -1;
		for (int resolution : ALL_RESOLUTIONS) {
			double distance = abs((options.maxCodeSize / (resolution + 2.0)) - rawCellSize);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestResolution = resolution;
			}
		}
		return bestResolution;
	}

	inline double getCellSize(int resolution) {
		return options.maxCodeSize / (resolution + 2.0);
	}

	inline double getBorderColor(const Mat& binaryInput, double cellSize) {
		return color_extractor::getHollowSquare(binaryInput, options.maxCodeSize, cellSize);
	}

	inline bool getCornerTemplateColors(const Mat& rgbInput, double cellSize, int resolution, Color* output) {
		int cornerColors = ALL_CORNER_COLORS;
		Color topLeft = Color::getClosest(color_extractor::getCell(rgbInput, cellSize, 1, 1));
		if ((cornerColors & topLeft.getBitMask()) == 0) {
			return false; //TODO only require 3 out of 4 pairs to be correct maybe?
		}

		Color bottomRight = Color::getClosest(color_extractor::getCell(rgbInput, cellSize, resolution, resolution));
		if (!bottomRight.isTemplateOpposite(topLeft)) {
			return false;
		}

		cornerColors ^= topLeft.getBitMask();
		cornerColors ^= bottomRight.getBitMask();
		Color bottomLeft = Color::getClosest(color_extractor::getCell(rgbInput, cellSize, 1, resolution));
		if ((cornerColors & bottomLeft.getBitMask()) == 0) {
			return false;
		}

		Color topRight = Color::getClosest(color_extractor::getCell(rgbInput, cellSize, resolution, 1));
		if (!topRight.isTemplateOpposite(bottomLeft)) {
			return false;
		}

		output[0] = topLeft;
		output[1] = bottomLeft;
		output[2] = bottomRight;
		output[3] = topRight;
		return true;
	}

private:
	inline int findFirstNonBlack(const Mat& binaryInput, int maxSteps, int startX, int startY,
										int stepX, int stepY, int offsetX, int offsetY) {
		int scanWidth = options.blackScanLength;
		int scanHeight = options.blackScanLength - 1;
		int steps = -1;
		while (++steps < maxSteps) {
			int x = startX + steps * stepX;
			int y = startY + steps * stepY;

			double value = color_extractor::get(binaryInput, x + offsetX, y, scanWidth, 1).val[0]
						   + color_extractor::get(binaryInput, x, y + offsetY, 1, scanHeight).val[0];
			if (value / 2 > options.blackThreshold) {
				return steps;
			}
		}
		return steps;
	}
};

const int FeatureExtractor::ALL_RESOLUTIONS[11];
int FeatureExtractor::ALL_CORNER_COLORS = Color::WHITE.getBitMask() | Color::AQUA.getBitMask()
										  | Color::YELLOW.getBitMask() | Color::GREEN.getBitMask();

}

#endif //CMCM_NATIVE_FEATUREEXTRACTOR_H
