package hu.trigary.cmcm.app.receive.processor.java;

import org.opencv.core.Size;

/**
 * Options and constants regarding how images should be processed.
 */
public class ProcessorOptions {
	public final Size imageSize;
	public final double minContourArea;
	public final int maxCodeSize;
	public final Size thresholdBlurSize = new Size(3, 3);
	public final double approxPolygonalEpsilon = 0.04;
	public final int blackScanLength = 13;
	public final double blackThreshold = 255 * 0.15;
	
	public ProcessorOptions(int imageHeight, int imageWidth) {
		imageSize = new Size(imageWidth, imageHeight);
		minContourArea = imageSize.area() * 0.1;
		maxCodeSize = Math.min(imageHeight, imageWidth);
	}
}
