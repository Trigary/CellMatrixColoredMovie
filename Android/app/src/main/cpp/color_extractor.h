#ifndef CMCM_NATIVE_COLOR_EXTRACTOR_H
#define CMCM_NATIVE_COLOR_EXTRACTOR_H

#include <opencv2/core/types.hpp>
#include <opencv2/core/mat.hpp>

using namespace cv;

namespace cmcm {
namespace color_extractor {

//TODO this is called a lot and I create a new (sub?) Mat every time - optimize?
Scalar get(const Mat& input, int x, int y, int width, int height) {
	return mean(input.operator()(Rect(x, y, width, height)));
}

double getHollowSquare(const Mat& singleInput, int inputSize, double size) {
	int intSize = (int) size;
	double color = 0;
	color += get(singleInput, 0, 0, inputSize, intSize).val[0]; //top
	color += get(singleInput, 0, inputSize - intSize, inputSize, intSize).val[0]; //bottom
	color += get(singleInput, 0, intSize, intSize, inputSize - (int) (2 * size)).val[0]; //left
	color += get(singleInput, inputSize - intSize, intSize, intSize, inputSize - (int) (2 * size)).val[0]; //right
	return color / 4;
}

Scalar getCell(const Mat& input, double cellSize, int x, int y) {
	int divider = 10;
	int padding = (int) (cellSize / divider);
	int size = (int) (cellSize - cellSize * 2 / divider);
	return get(input, (int) (x * cellSize) + padding, (int) (y * cellSize) + padding, size, size);
}

}
}

#endif //CMCM_NATIVE_COLOR_EXTRACTOR_H
