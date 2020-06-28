package hu.trigary.cmcm.app.receive.processor;

import android.util.Log;
import hu.trigary.cmcm.app.util.BenchmarkUtils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

/**
 * A frame processor thread that uses the native image processing implementation.
 */
public class FrameProcessorThreadNative extends FrameProcessorThread {
	
	public FrameProcessorThreadNative(int id, FrameProcessorManager manager) {
		super(id, manager);
	}
	
	@Override
	public final void run() {
		Size inputSize = manager.getInputResolution();
		Size notRotatedSize = manager.getNotRotatedOutputResolution();
		Size rotatedSize = manager.getRotatedOutputResolution();
		//int yuvToRgb = manager.getYuvToRgbCode(); //TODO use this
		
		Log.d("CMCM", "Calling native method: init");
		long handle = init(manager, (int) inputSize.height, (int) inputSize.width, (int) notRotatedSize.height,
				(int) notRotatedSize.width, (int) rotatedSize.height, (int) rotatedSize.width);
		Log.d("CMCM", "Finished native method: init");
		Log.d("CMCM", "Created handle: " + handle);
		Mat outputMat = new Mat(getOutputMatAddress(handle));
		
		try {
			synchronized (this) {
				manager.onThreadReady(this);
				
				//noinspection InfiniteLoopStatement
				while (true) {
					wait();
					long start = System.nanoTime();
					Log.d("CMCM", "Calling native method: process");
					process(handle, buffer);
					Log.d("CMCM", "Finished native method: process");
					BenchmarkUtils.update("Process", start);
					manager.onThreadDone(this, start, outputMat);
				}
			}
		} catch (InterruptedException ignored) {
		} finally {
			Log.d("CMCM", "Releasing handle: " + handle);
			Log.d("CMCM", "Calling native method: release");
			release(handle);
			Log.d("CMCM", "Finished native method: release");
		}
	}
	
	private static native long init(FrameProcessorManager manager, int inputHeight, int inputWidth, int notRotatedHeight,
			int notRotatedWidth, int rotatedHeight, int rotatedWidth);
	
	private static native long getOutputMatAddress(long handle);
	
	private static native void process(long handle, byte[] buffer);
	
	private static native void release(long handle);
}
