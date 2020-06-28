package hu.trigary.cmcm.app.receive.processor;

import hu.trigary.cmcm.app.receive.processor.java.FrameProcessor;
import hu.trigary.cmcm.app.util.BenchmarkUtils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * A frame processor thread that uses the Java image processing implementation.
 */
public class FrameProcessorThreadJava extends FrameProcessorThread {
	
	public FrameProcessorThreadJava(int id, FrameProcessorManager manager) {
		super(id, manager);
	}
	
	@Override
	public void run() {
		Size inputSize = manager.getInputResolution();
		Mat yuvMat = new Mat((int) (inputSize.height * 1.5), (int) inputSize.width, CvType.CV_8UC1);
		Mat rgbMat = new Mat(inputSize, CvType.CV_8UC3);
		Size notRotatedSize = manager.getNotRotatedOutputResolution();
		Mat resizedMat = new Mat(notRotatedSize, CvType.CV_8UC3);
		Mat rotatedMat = new Mat(manager.getRotatedOutputResolution(), CvType.CV_8UC3);
		
		try (FrameProcessor processor = new FrameProcessor(manager, rotatedMat.height(), rotatedMat.width())) {
			synchronized (this) {
				manager.onThreadReady(this);
				
				//noinspection InfiniteLoopStatement
				while (true) {
					wait();
					long start = System.nanoTime();
					
					yuvMat.put(0, 0, buffer);
					Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21);
					Imgproc.resize(rgbMat, resizedMat, notRotatedSize, 0, 0, Imgproc.INTER_NEAREST);
					Core.rotate(resizedMat, rotatedMat, Core.ROTATE_90_CLOCKWISE);
					
					processor.process(rotatedMat);
					BenchmarkUtils.update("Process", start);
					
					manager.onThreadDone(this, start, rotatedMat);
				}
			}
		} catch (InterruptedException ignored) {
		} finally {
			yuvMat.release();
			rgbMat.release();
			resizedMat.release();
			rotatedMat.release();
		}
	}
}
