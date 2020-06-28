package hu.trigary.cmcm.app.receive.processor.java;

import hu.trigary.cmcm.app.receive.processor.FrameProcessorManager;
import hu.trigary.cmcm.library.utilities.Color;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class FrameProcessor implements Closeable {
	private static final CellColor[] CORNER_COLORS = {CellColor.WHITE, CellColor.AQUA, CellColor.YELLOW, CellColor.GREEN};
	private final FrameProcessorManager manager;
	private final ProcessorOptions options;
	private final FeatureExtractor featureExtractor;
	private final List<MatOfPoint> allContours = new ArrayList<>();
	private final List<MatOfPoint> filteredContours = new ArrayList<>();
	private final CellColor[] templateColors = new CellColor[8];
	private int resolution;
	private double cellSize;
	private int rotateClockwise;
	private boolean flipVertical;
	private volatile boolean pause;
	
	public FrameProcessor(FrameProcessorManager manager, int inputHeight, int inputWidth) {
		this.manager = manager;
		options = new ProcessorOptions(inputHeight, inputWidth);
		featureExtractor = new FeatureExtractor(options);
	}
	
	@Override
	public void close() {
		pause = true;
	}
	
	/**
	 * Processes the specified image.
	 */
	public void process(Mat rgbInput) {
		featureExtractor.findContours(rgbInput, allContours);
		featureExtractor.filterContours(allContours, filteredContours);
		//TODO no need to use lists, just do the filtering processing in the same loop
		
		if (!filteredContours.isEmpty()) {
			handleContours(rgbInput);
			filteredContours.clear();
		}
		
		allContours.clear();
	}
	
	private void handleContours(Mat rgbInput) {
		Mat extractedRgb = new Mat();
		Mat extractedBinary = new Mat();
		for (int contourIndex = 0; contourIndex < filteredContours.size(); contourIndex++) {
			MatOfPoint contour = filteredContours.get(contourIndex);
			featureExtractor.warpPerspective(rgbInput, extractedRgb, contour);
			
			featureExtractor.toBinary(extractedRgb, extractedBinary);
			int result = tryContour(extractedRgb, extractedBinary);
			if (result == 1) { //border doesn't match
				Imgproc.drawContours(rgbInput, filteredContours, contourIndex, new Scalar(255, 0, 0), 3);
			} else if (result == 2) { //border matches, template cells don't
				Imgproc.drawContours(rgbInput, filteredContours, contourIndex, new Scalar(0, 255, 0), 3);
			} else { //everything matches
				Imgproc.drawContours(rgbInput, filteredContours, contourIndex, new Scalar(0, 0, 255), 3);
				handleGoodContour(extractedRgb);
			}
			
			contour.release();
		}
		
		extractedRgb.release();
		extractedBinary.release();
	}
	
	private int tryContour(Mat extractedRgb, Mat extractedBinary) {
		resolution = featureExtractor.getResolution(extractedBinary);
		cellSize = featureExtractor.getCellSize(resolution);
		if (featureExtractor.getBorderColor(extractedBinary, cellSize) > options.blackThreshold) {
			return 1;
		}
		
		if (!featureExtractor.getCornerTemplateColors(extractedRgb, cellSize, resolution, templateColors)) {
			return 2;
		}
		
		//find the most likely rotation, if there are 3 different ones then abort
		//TODO use the rest of template cells
		int valueA = -1;
		int valueB = -1;
		int countA = 0;
		int countB = 0;
		for (int i = 0; i < CORNER_COLORS.length; i++) {
			CellColor cornerColor = CORNER_COLORS[i];
			for (int j = 0; j < 4; j++) {
				if (templateColors[j] != cornerColor) {
					continue;
				}
				int value = (j + 4 - i) % 4;
				if (valueA == value || valueA < 0) {
					valueA = value;
					countA++;
				} else if (valueB == value || valueB < 0) {
					valueB = value;
					countB++;
				}
			}
		}
		if (countA >= 3) {
			rotateClockwise = valueA;
		} else if (countB >= 3) {
			rotateClockwise = valueB;
		} else {
			return 2;
		}
		
		/*rotateClockwise = 3;
		if (templateColors[0] == CellColor.WHITE) {
			rotateClockwise = 0;
		} else if (templateColors[1] == CellColor.WHITE) {
			rotateClockwise = 1;
		} else if (templateColors[2] == CellColor.WHITE) {
			rotateClockwise = 2;
		}*/
		
		
		flipVertical = false;
		if (templateColors[(rotateClockwise + 1) % 4] != CellColor.AQUA) {
			rotateClockwise += rotateClockwise == 0 || rotateClockwise == 2 ? 1 : -1;
			flipVertical = true;
		}
		
		//TODO maybe calibrate what RGB value maps to which color of the 8 ones based on the RBG values of the template cells?
		// but what if the template cells are damaged? this has to be done with moderation
		// -> maybe exponentially moving smoothing average
		return 0;
	}
	
	private void handleGoodContour(Mat extractedRgb) {
		if (pause) {
			return;
		}
		
		Color[] pixels = new Color[resolution * resolution];
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
				
				pixels[realX + realY * resolution] = CellColor.getClosest(ColorExtractor
						.getCell(extractedRgb, cellSize, x + 1, y + 1)).getColor();
			}
		}
		
		manager.onPixelsExtracted(resolution, pixels);
	}
}
