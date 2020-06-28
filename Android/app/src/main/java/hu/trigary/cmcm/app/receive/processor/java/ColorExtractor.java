package hu.trigary.cmcm.app.receive.processor.java;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

/**
 * Utility class that gets the average color on areas of different shapes.
 */
public final class ColorExtractor {
	
	private ColorExtractor() { }
	
	public static Scalar get(Mat input, int x, int y, int width, int height) {
		Mat temp = input.submat(new Rect(x, y, width, height));
		Scalar result = Core.mean(temp);
		temp.release();
		return result;
	}
	
	public static double getHollowSquare(Mat singleInput, int inputSize, double size) {
		int intSize = (int) size;
		double color = 0;
		color += get(singleInput, 0, 0, inputSize, intSize).val[0]; //top
		color += get(singleInput, 0, inputSize - intSize, inputSize, intSize).val[0]; //bottom
		color += get(singleInput, 0, intSize, intSize, inputSize - (int) (2 * size)).val[0]; //left
		color += get(singleInput, inputSize - intSize, intSize, intSize, inputSize - (int) (2 * size)).val[0]; //right
		return color / 4;
	}
	
	public static Scalar getCell(Mat input, double cellSize, int x, int y) {
		int divider = 10;
		int padding = (int) (cellSize / divider);
		int size = (int) (cellSize - cellSize * 2 / divider);
		return get(input, (int) (x * cellSize) + padding, (int) (y * cellSize) + padding, size, size);
	}
}
