package hu.trigary.cmcm.app.receive.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import hu.trigary.cmcm.app.MainActivity;
import hu.trigary.cmcm.app.receive.processor.FrameProcessorManager;
import hu.trigary.cmcm.app.util.BenchmarkUtils;
import hu.trigary.cmcm.app.util.IntSize;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * The class responsible for providing a common interface for the old and new Android camera API,
 * as well as letting the processed camera frames be displayed on the screen.
 */
public abstract class DisplayCameraHandler {
	private static final IntSize ROTATED_DISPLAY_SIZE = new IntSize(360, 640);
	//TODO this resolution is too low, but we also must think about performance
	protected final FrameProcessorManager processorManager;
	protected final Handler captureCallbackHandler;
	private final Object displayLock = new Object();
	private SurfaceHolder displayHolder;
	private Bitmap outputBitmap;
	private float displayScaleX;
	private float displayScaleY;
	
	protected DisplayCameraHandler(int yuvFormatCode) {
		processorManager = new FrameProcessorManager(this, ROTATED_DISPLAY_SIZE, yuvFormatCode);
		HandlerThread thread = new HandlerThread("CMCM:CameraHandler", Process.THREAD_PRIORITY_FOREGROUND);
		thread.start();
		captureCallbackHandler = new Handler(thread.getLooper());
	}
	
	@NonNull
	public static DisplayCameraHandler create(@NonNull Context context) {
		return Build.VERSION.SDK_INT >= 21 && MainActivity.USE_NEW_CAMERA_API
				? new NewCameraHandler(context)
				: new OldCameraHandler();
	}
	
	public final void onSurfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
		captureCallbackHandler.post(() -> {
			synchronized (displayLock) {
				displayHolder = surfaceHolder;
				outputBitmap = Bitmap.createBitmap(ROTATED_DISPLAY_SIZE.getWidth(),
						ROTATED_DISPLAY_SIZE.getHeight(), Bitmap.Config.ARGB_8888);
				
				Rect display = surfaceHolder.getSurfaceFrame();
				displayScaleX = (float) display.width() / ROTATED_DISPLAY_SIZE.getWidth();
				displayScaleY = (float) display.height() / ROTATED_DISPLAY_SIZE.getHeight();
			}
			
			IntSize inputSize = resume();
			Log.d("CMCM", "Camera resolution: " + inputSize);
			processorManager.tryInitialize(inputSize);
		});
	}
	
	public final void onSurfaceDestroyed() {
		pause();
		synchronized (displayLock) {
			displayHolder = null;
			outputBitmap = null;
		}
	}
	
	public final void onDestroyed() {
		BenchmarkUtils.clear();
		processorManager.close();
		captureCallbackHandler.getLooper().quit();
	}
	
	/**
	 * Called when image capturing is about to (re)start.
	 *
	 * @return the resolution of the images that will be provided by the camera
	 */
	@NonNull
	protected abstract IntSize resume();
	
	/**
	 * Called when image capturing is being stopped.
	 */
	protected abstract void pause();
	
	/**
	 * Attempts to display the specified image on the screen.
	 *
	 * @param image the image to display
	 * @return true only if the image was successfully displayed
	 */
	public final boolean tryDisplayImage(@NonNull Mat image) {
		synchronized (displayLock) {
			if (displayHolder == null) {
				return false;
			}
			
			Canvas canvas = displayHolder.lockCanvas();
			if (canvas == null) {
				return false;
			}
			
			Utils.matToBitmap(image, outputBitmap);
			canvas.scale(displayScaleX, displayScaleY);
			canvas.drawBitmap(outputBitmap, 0, 0, null);
			displayHolder.unlockCanvasAndPost(canvas);
			return true;
		}
	}
	
	/**
	 * Gives the camera a buffer it can fill an image with.
	 *
	 * @param buffer the buffer to add
	 */
	public abstract void addCallbackBuffer(@NonNull byte[] buffer);
}
