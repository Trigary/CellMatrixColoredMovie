package hu.trigary.cmcm.app.receive.camera;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import hu.trigary.cmcm.app.util.IntSize;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

/**
 * The class responsible for interacting with the new Android camera API.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NewCameraHandler extends DisplayCameraHandler {
	private static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;
	private final Queue<byte[]> bufferQueue = new ArrayDeque<>();
	private final CameraManager cameraManager;
	private final String cameraId = "0";
	private final NewCameraConfigurator cameraConfigurator;
	private CameraDevice activeCamera;
	private ImageReader imageReader;
	
	public NewCameraHandler(@NonNull Context context) {
		super(IMAGE_FORMAT);
		try {
			cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
			cameraConfigurator = new NewCameraConfigurator(cameraManager.getCameraCharacteristics(cameraId));
		} catch (CameraAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@NonNull
	@Override
	@SuppressLint("MissingPermission")
	public synchronized IntSize resume() {
		try {
			cameraManager.openCamera(cameraId, new CameraStateCallback(), null);
			Size size = cameraConfigurator.getSize();
			return new IntSize(size.getWidth(), size.getHeight());
		} catch (CameraAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public synchronized void pause() {
		bufferQueue.clear();
		if (activeCamera != null) {
			activeCamera.close();
			activeCamera = null;
			
			if (imageReader != null) {
				imageReader.close();
				imageReader = null;
			}
		}
	}
	
	@Override
	public void addCallbackBuffer(@NonNull byte[] buffer) {
		synchronized (bufferQueue) {
			bufferQueue.add(buffer);
		}
	}
	
	private class CameraStateCallback extends CameraDevice.StateCallback {
		@Override
		public void onOpened(@NonNull CameraDevice camera) {
			synchronized (NewCameraHandler.this) {
				activeCamera = camera;
				
				try {
					Size size = cameraConfigurator.getSize();
					imageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), IMAGE_FORMAT, 1);
					imageReader.setOnImageAvailableListener(new ImageAvailableListener(), captureCallbackHandler);
					camera.createCaptureSession(Collections.singletonList(imageReader.getSurface()),
							new CaptureStateCallback(), captureCallbackHandler);
				} catch (CameraAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		@Override
		public void onDisconnected(@NonNull CameraDevice camera) { }
		
		@Override
		public void onError(@NonNull CameraDevice camera, int error) {
			Log.d("CMCM", "camera state error: " + error);
		}
	}
	
	private class CaptureStateCallback extends CameraCaptureSession.StateCallback {
		@Override
		public void onConfigured(@NonNull CameraCaptureSession session) {
			synchronized (NewCameraHandler.this) {
				try {
					CaptureRequest.Builder builder = activeCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
					builder.addTarget(imageReader.getSurface());
					cameraConfigurator.configureRequest(builder);
					session.setRepeatingRequest(builder.build(), null, captureCallbackHandler);
				} catch (CameraAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		@Override
		public void onConfigureFailed(@NonNull CameraCaptureSession session) {
			Log.d("CMCM", "configuring session failed");
		}
	}
	
	private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
		@Override
		public void onImageAvailable(ImageReader reader) {
			byte[] buffer;
			synchronized (bufferQueue) {
				buffer = bufferQueue.poll();
				if (buffer == null) {
					return;
				}
			}
			
			//TODO I regularly get warnings that unable to lock on buffer, is there anything I could do?
			try (Image image = reader.acquireLatestImage()) {
				imageToByteArray(image, buffer);
				processorManager.onFrameAvailable(buffer);
			}
		}
		
		//int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
		private void imageToByteArray(@NonNull Image image, @NonNull byte[] output) {
			Rect crop = image.getCropRect();
			int width = crop.width();
			int height = crop.height();
			
			Image.Plane[] planes = image.getPlanes();
			byte[] rowData = new byte[planes[0].getRowStride()];
			
			for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
				int channelOffset;
				int outputStride;
				if (planeIndex == 0) {
					channelOffset = 0;
					outputStride = 1;
				} else if (planeIndex == 1) {
					channelOffset = width * height + 1;
					outputStride = 2;
				} else {
					channelOffset = width * height;
					outputStride = 2;
				}
				
				ByteBuffer buffer = planes[planeIndex].getBuffer();
				int rowStride = planes[planeIndex].getRowStride();
				int pixelStride = planes[planeIndex].getPixelStride();
				
				int shift = (planeIndex == 0) ? 0 : 1;
				int widthShifted = width >> shift;
				int heightShifted = height >> shift;
				
				buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
				
				for (int row = 0; row < heightShifted; row++) {
					int length;
					
					if (pixelStride == 1 && outputStride == 1) {
						length = widthShifted;
						buffer.get(output, channelOffset, length);
						channelOffset += length;
					} else {
						length = (widthShifted - 1) * pixelStride + 1;
						buffer.get(rowData, 0, length);
						
						for (int col = 0; col < widthShifted; col++) {
							output[channelOffset] = rowData[col * pixelStride];
							channelOffset += outputStride;
						}
					}
					
					if (row < heightShifted - 1) {
						buffer.position(buffer.position() + rowStride - length);
					}
				}
			}
		}
	}
}
