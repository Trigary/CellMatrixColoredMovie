package hu.trigary.cmcm.app.receive.camera;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import hu.trigary.cmcm.app.util.IntSize;

import java.io.IOException;

/**
 * The class responsible for interacting with the old Android camera API.
 * @noinspection deprecation
 */
public class OldCameraHandler extends DisplayCameraHandler implements Camera.PreviewCallback {
	public static final int IMAGE_FORMAT = ImageFormat.NV21;
	private final SurfaceTexture dummyTexture = new SurfaceTexture(1);
	private Camera camera;
	private OldCameraConfigurator configurator;
	
	public OldCameraHandler() {
		super(IMAGE_FORMAT);
	}
	
	@NonNull
	@Override
	protected synchronized IntSize resume() {
		camera = Camera.open();
		try {
			camera.setPreviewTexture(dummyTexture);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (configurator == null) {
			configurator = new OldCameraConfigurator(camera);
		}
		configurator.setParameters(camera);
		
		Camera.Size camSize = camera.getParameters().getPreviewSize();
		IntSize size = new IntSize(camSize.width, camSize.height);
		
		camera.setPreviewCallbackWithBuffer(this);
		camera.startPreview();
		return size;
	}
	
	@Override
	protected synchronized void pause() {
		camera.stopPreview();
		camera.release();
		camera = null;
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		processorManager.onFrameAvailable(data);
	}
	
	@Override
	public synchronized void addCallbackBuffer(@NonNull byte[] buffer) {
		if (camera != null) {
			camera.addCallbackBuffer(buffer);
		}
	}
}
