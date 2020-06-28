package hu.trigary.cmcm.app.receive.processor.java;

import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A class that is able to extract various information from an image,
 * or convert an image into something else.
 */
public class FeatureExtractor implements Closeable {
	private static final int ALL_CORNER_COLORS = CellColor.WHITE.getBitmask() | CellColor.AQUA.getBitmask()
			| CellColor.YELLOW.getBitmask() | CellColor.GREEN.getBitmask();
	private final ProcessorOptions options;
	private final Mat contoursMat;
	private final MatOfPoint2f pointsMat = new MatOfPoint2f();
	private final Comparator<Point> pointComparator = (one, two) -> one.y < two.y ? -1 : 1;
	private final MatOfPoint2f destinationMat;
	private final MatOfPoint2f cornersMat = new MatOfPoint2f();
	private final Size warpSize;
	
	public FeatureExtractor(ProcessorOptions options) {
		this.options = options;
		contoursMat = new Mat(options.imageSize, CvType.CV_8UC1);
		
		int size = options.maxCodeSize;
		Point[] destination = {new Point(0, 0), new Point(size, 0), new Point(0, size), new Point(size, size)};
		destinationMat = new MatOfPoint2f(destination);
		warpSize = new Size(size, size);
	}
	
	@Override
	public void close() {
		contoursMat.release();
		pointsMat.release();
		destinationMat.release();
		cornersMat.release();
	}
	
	/**
	 * Converts and RGB image to a single channel one using the Otsu threshold method.
	 */
	public void toBinary(Mat rgbInput, Mat binaryOutput) {
		Imgproc.cvtColor(rgbInput, binaryOutput, Imgproc.COLOR_RGB2GRAY);
		Imgproc.GaussianBlur(binaryOutput, binaryOutput, options.thresholdBlurSize, 0);
		Imgproc.threshold(binaryOutput, binaryOutput, 0, 255, Imgproc.THRESH_OTSU);
	}
	
	/**
	 * Finds all contours in the image.
	 */
	public void findContours(Mat rgbInput, List<MatOfPoint> output) {
		toBinary(rgbInput, contoursMat);
		Imgproc.findContours(contoursMat, output, contoursMat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		//TODO if I don't do the chain approximation here,
		//I MIGHT (untested) get a boost in performance and approxPolyDP can take care of that
	}
	
	/**
	 * Filters the specified contours so that only possible movie frames are matched:
	 * - the count of corners are checked
	 * - the area is checked
	 * - convexity is checked
	 */
	public void filterContours(List<MatOfPoint> input, List<MatOfPoint> output) {
		for (MatOfPoint contour : input) {
			if (contour.total() < 4 || Imgproc.contourArea(contour) < options.minContourArea) {
				contour.release();
				continue;
			}
			
			contour.convertTo(pointsMat, CvType.CV_32FC2);
			Imgproc.approxPolyDP(pointsMat, pointsMat, options.approxPolygonalEpsilon * contour.total(), true);
			if (pointsMat.total() != 4) {
				contour.release();
				continue;
			}
			
			pointsMat.convertTo(contour, CvType.CV_32S);
			if (!Imgproc.isContourConvex(contour)) {
				contour.release();
				continue;
			}
			
			//TODO do some other stuff? What does eg. AruCo (opencv_contrib) or zxing QR code do?
			output.add(contour);
		}
	}
	
	/**
	 * Applies perspective transformation to compensate for the fact that the camera might not
	 * be directly above the screen, it might not be looking directly at the screen at 90 degrees.
	 */
	public void warpPerspective(Mat input, Mat output, MatOfPoint contour) {
		Point[] corners = contour.toArray();
		Arrays.sort(corners, pointComparator);
		if (corners[0].x > corners[1].x) {
			Point temp = corners[0];
			corners[0] = corners[1];
			corners[1] = temp;
		}
		if (corners[2].x > corners[3].x) {
			Point temp = corners[2];
			corners[2] = corners[3];
			corners[3] = temp;
		}
		
		cornersMat.fromArray(corners);
		Mat perspective = Imgproc.getPerspectiveTransform(cornersMat, destinationMat);
		Imgproc.warpPerspective(input, output, perspective, warpSize, Imgproc.INTER_NEAREST);
		perspective.release();
	}
	
	/**
	 * Determines the resolution based on the black border's width.
	 */
	public int getResolution(Mat binaryInput) {
		int pixelCount = options.maxCodeSize;
		int skipPixels = pixelCount / 130;
		int maxSteps = (pixelCount / 10) - skipPixels;
		
		double rawCellSize = 0;
		int farEnd = pixelCount - skipPixels - 1;
		int offset = options.blackScanLength - 1;
		rawCellSize += 1d / findFirstNonBlack(binaryInput, maxSteps,
				skipPixels, skipPixels, 1, 1, 0, 1); //top left
		rawCellSize += 1d / findFirstNonBlack(binaryInput, maxSteps,
				skipPixels, farEnd, 1, -1, 0, -1 - offset); //bottom left
		rawCellSize += 1d / findFirstNonBlack(binaryInput, maxSteps,
				farEnd, skipPixels, -1, 1, -offset, 1); //top right
		rawCellSize += 1d / findFirstNonBlack(binaryInput, maxSteps,
				farEnd, farEnd, -1, -1, -offset, -1 - offset); //bottom right
		rawCellSize = skipPixels + (1 / (rawCellSize / 4));
		
		double bestDistance = Double.MAX_VALUE;
		int bestResolution = -1;
		for (FrameInfo.ContentResolution resolution : FrameInfo.ContentResolution.values()) {
			double distance = Math.abs((options.maxCodeSize / (resolution.getValue() + 2d)) - rawCellSize);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestResolution = resolution.getValue();
			}
		}
		return bestResolution;
	}
	
	private int findFirstNonBlack(Mat binaryInput, int maxSteps, int startX, int startY,
			int stepX, int stepY, int offsetX, int offsetY) {
		int scanWidth = options.blackScanLength;
		int scanHeight = options.blackScanLength - 1;
		int steps = -1;
		while (++steps < maxSteps) {
			int x = startX + steps * stepX;
			int y = startY + steps * stepY;
			
			double value = ColorExtractor.get(binaryInput, x + offsetX, y, scanWidth, 1).val[0]
					+ ColorExtractor.get(binaryInput, x, y + offsetY, 1, scanHeight).val[0];
			if (value / 2 > options.blackThreshold) {
				return steps;
			}
		}
		return steps;
	}
	
	/**
	 * Gets the size of a cell in an extracted where the movie frame has the specified resolution.
	 */
	public double getCellSize(int resolution) {
		return options.maxCodeSize / (resolution + 2d);
	}
	
	/**
	 * Gets the color (blackness) of the border of the specified image.
	 */
	public double getBorderColor(Mat binaryInput, double cellSize) {
		return ColorExtractor.getHollowSquare(binaryInput, options.maxCodeSize, cellSize);
	}
	
	/**
	 * Gets whether the specified parameters pass the template cell color test.
	 */
	public boolean getCornerTemplateColors(Mat rgbInput, double cellSize, int resolution, CellColor[] container) {
		//TODO use the rest of template cells
		CellColor topLeft = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, 1, 1));
		CellColor topRight = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, resolution, 1));
		CellColor bottomLeft = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, 1, resolution));
		CellColor bottomRight = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, resolution, resolution));
		
		if (topLeft.getTemplateOpposite() != bottomRight && topRight.getTemplateOpposite() != bottomLeft) {
			return false;
		}
		
		container[0] = topLeft;
		container[1] = bottomLeft;
		container[2] = bottomRight;
		container[3] = topRight;
		
		int counter = 0;
		int cornerColors = ALL_CORNER_COLORS;
		for (int i = 0; i < 4; i++) {
			CellColor cellColor = container[i];
			if ((cornerColors & cellColor.getBitmask()) == 0) {
				counter++;
			} else {
				cornerColors ^= cellColor.getBitmask();
			}
		}
		return counter <= 1;
		
		/*int cornerColors = ALL_CORNER_COLORS;
		CellColor topLeft = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, 1, 1));
		if ((cornerColors & topLeft.getBitmask()) == 0) {
			return false;
		}
		
		CellColor bottomRight = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, resolution, resolution));
		if (bottomRight != topLeft.getTemplateOpposite()) {
			return false;
		}
		
		cornerColors ^= topLeft.getBitmask();
		cornerColors ^= bottomRight.getBitmask();
		CellColor bottomLeft = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, 1, resolution));
		if ((cornerColors & bottomLeft.getBitmask()) == 0) {
			return false;
		}
		
		CellColor topRight = CellColor.getClosest(ColorExtractor.getCell(rgbInput, cellSize, resolution, 1));
		if (topRight != bottomLeft.getTemplateOpposite()) {
			return false;
		}
		
		container[0] = topLeft;
		container[1] = bottomLeft;
		container[2] = bottomRight;
		container[3] = topRight;
		return true;*/
	}
}
