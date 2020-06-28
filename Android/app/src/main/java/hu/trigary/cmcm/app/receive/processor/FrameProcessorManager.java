package hu.trigary.cmcm.app.receive.processor;

import android.graphics.ImageFormat;
import android.support.annotation.NonNull;
import android.util.Log;
import hu.trigary.cmcm.app.MainActivity;
import hu.trigary.cmcm.app.display.DisplayTicketFragment;
import hu.trigary.cmcm.app.display.DisplayUserFragment;
import hu.trigary.cmcm.app.receive.camera.DisplayCameraHandler;
import hu.trigary.cmcm.app.util.BenchmarkUtils;
import hu.trigary.cmcm.app.util.IntSize;
import hu.trigary.cmcm.library.parser.MovieParser;
import hu.trigary.cmcm.library.utilities.Color;
import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The class responsible for keeping track of frame processor threads,
 * for timing the image processing to ensure a constant and steady stream of frames
 * and for selecting a thread for frames acquired from the camera.
 */
public class FrameProcessorManager implements Closeable {
	private static final int NANO_SKIP_COUNT = 10;
	private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	private final Map<Integer, MovieParser> parsers = new ConcurrentHashMap<>();
	private final Queue<FrameProcessorThread> threadQueue = new ArrayDeque<>();
	private final DisplayCameraHandler cameraHandler;
	private final Size notRotatedOutputResolution;
	private final Size rotatedOutputResolution;
	private final int yuvFormatCode;
	private Size inputResolution;
	private int skippedNanos;
	private long lastStartNanos = System.nanoTime();
	private long averageDeltaNanos = -1;
	private boolean closing;
	
	public FrameProcessorManager(DisplayCameraHandler cameraHandler, IntSize rotatedOutputResolution, int yuvFormatCode) {
		this.cameraHandler = cameraHandler;
		notRotatedOutputResolution = new Size(rotatedOutputResolution.getHeight(),
				rotatedOutputResolution.getWidth());
		this.rotatedOutputResolution = new Size(rotatedOutputResolution.getWidth(),
				rotatedOutputResolution.getHeight());
		this.yuvFormatCode = yuvFormatCode;
	}
	
	public synchronized void tryInitialize(@NonNull IntSize inputResolution) {
		if (this.inputResolution == null) {
			this.inputResolution = new Size(inputResolution.getWidth(), inputResolution.getHeight());
			for (int i = 0; i < THREAD_COUNT; i++) {
				FrameProcessorThread.create(i, this).start();
			}
		} else {
			for (FrameProcessorThread thread : threadQueue) {
				cameraHandler.addCallbackBuffer(thread.getBuffer());
			}
		}
	}
	
	@Override
	public synchronized void close() {
		closing = true;
		for (FrameProcessorThread thread : threadQueue) {
			thread.interrupt();
		}
	}
	
	public Size getInputResolution() {
		return inputResolution;
	}
	
	public Size getNotRotatedOutputResolution() {
		return notRotatedOutputResolution;
	}
	
	public Size getRotatedOutputResolution() {
		return rotatedOutputResolution;
	}
	
	/**
	 * Called when a thread is ready to begin processing frames.
	 *
	 * @param thread the thread that is ready
	 */
	public synchronized void onThreadReady(@NonNull FrameProcessorThread thread) {
		threadQueue.add(thread);
		cameraHandler.addCallbackBuffer(new byte[(int) (inputResolution.area()
				* ImageFormat.getBitsPerPixel(yuvFormatCode) / 8)]);
	}
	
	/**
	 * Called when a thread has completed processing a frame.
	 *
	 * @param thread the thread that is done
	 * @param startNanos the time at which the processing began
	 * @param mat the processed frame
	 */
	public synchronized void onThreadDone(@NonNull FrameProcessorThread thread, long startNanos, @NonNull Mat mat) {
		if (closing) {
			thread.interrupt();
			return;
		}
		
		long start = System.nanoTime();
		if (cameraHandler.tryDisplayImage(mat)) {
			BenchmarkUtils.update("Display", start);
		}
		
		threadQueue.add(thread);
		cameraHandler.addCallbackBuffer(thread.getBuffer());
		
		if (skippedNanos == NANO_SKIP_COUNT) {
			averageDeltaNanos += (System.nanoTime() - startNanos - averageDeltaNanos) * 0.1;
		} else if (++skippedNanos == NANO_SKIP_COUNT) {
			averageDeltaNanos = System.nanoTime() - startNanos;
		} //TODO possible better way of doing it, eg. step distance based on data count
	}
	
	/**
	 * Called when a frame is available for processing.
	 *
	 * @param buffer the buffer containing the frame
	 */
	public synchronized void onFrameAvailable(@NonNull byte[] buffer) {
		long current = System.nanoTime();
		BenchmarkUtils.update("FrameAvailable", current);
		
		if (current < lastStartNanos + (averageDeltaNanos / THREAD_COUNT)) {
			cameraHandler.addCallbackBuffer(buffer);
			return; //TODO test whether this works as intended
		}
		
		lastStartNanos = current;
		threadQueue.remove().onFrameReceived(buffer);
	}
	
	/**
	 * Called by the Java implementation when the pixels were extracted from a frame.
	 *
	 * @param resolution the resolution of the possibly parsable movie frame
	 * @param pixels the pixels of the possibly parsable movie frame
	 */
	public void onPixelsExtracted(int resolution, @NonNull Color[] pixels) {
		byte[] result = getParser(resolution).tryAddFrame(pixels);
		if (result != null) {
			onMovieDecoded(result);
		}
	}
	
	/**
	 * Called by the native implementation when the content chunks were extracted from a frame.
	 *
	 * @param resolution the resolution of the possibly parsable movie frame
	 * @param contentChunks the content chunks of the possibly parsable movie frame
	 */
	public void onContentChunksExtracted(int resolution, @NonNull int[] contentChunks) {
		byte[] result = getParser(resolution).tryAddFrame(contentChunks);
		if (result != null) {
			onMovieDecoded(result);
		}
	}
	
	@NonNull
	private MovieParser getParser(int resolution) {
		MovieParser parser = parsers.get(resolution);
		if (parser == null) {
			synchronized (parsers) {
				parser = parsers.get(resolution);
				if (parser == null) {
					//noinspection ConstantConditions
					parser = new MovieParser(new FrameInfo(FrameInfo.ContentResolution.fromValue(resolution)));
					parsers.put(resolution, parser);
				}
			}
		}
		return parser;
	}
	
	private synchronized void onMovieDecoded(@NonNull byte[] result) {
		if (!closing) {
			Log.d("CMCM", "Successfully decoded movie: " + new String(result, StandardCharsets.UTF_8));
			MainActivity.MAIN_HANDLER.post(() -> {
				if (MainActivity.USE_TICKET_DEMO) {
					DisplayTicketFragment.create(result).display();
				} else {
					DisplayUserFragment.create(result).display();
				}
			});
			close();
		}
	}
}
