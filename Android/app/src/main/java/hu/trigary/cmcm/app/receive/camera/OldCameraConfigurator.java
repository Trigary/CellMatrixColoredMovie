package hu.trigary.cmcm.app.receive.camera;

import android.hardware.Camera;
import android.support.annotation.NonNull;
import hu.trigary.cmcm.app.util.IntSize;

import java.util.ArrayList;
import java.util.List;

/**
 * @noinspection deprecation
 */
public class OldCameraConfigurator {
	private final IntSize size;
	
	public OldCameraConfigurator(@NonNull Camera camera) {
		List<Camera.Size> rawSizes = camera.getParameters().getSupportedPreviewSizes();
		List<IntSize> sizes = new ArrayList<>(rawSizes.size());
		for (Camera.Size raw : rawSizes) {
			sizes.add(new IntSize(raw.width, raw.height));
		}
		size = getBestSize(sizes);
	}
	
	/**
	 * Configures the specified camera.
	 *
	 * @param camera the camera to configure
	 */
	public void setParameters(@NonNull Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		parameters.setRecordingHint(true);
		parameters.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
		camera.setParameters(parameters);
		parameters = camera.getParameters();
		
		parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
		parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
		parameters.setAntibanding(Camera.Parameters.ANTIBANDING_60HZ);
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		
		if (parameters.isZoomSupported()) {
			parameters.setZoom(0);
		}
		
		if (parameters.isAutoExposureLockSupported()) {
			parameters.setAutoExposureLock(false);
		}
		//parameters.setExposureCompensation(Camera.Parameters.);
		
		if (parameters.isAutoWhiteBalanceLockSupported()) {
			parameters.setAutoWhiteBalanceLock(true);
		} else {
			parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
		}
		
		if (parameters.isVideoStabilizationSupported()) {
			parameters.setVideoStabilization(true);
		}
		
		parameters.setPreviewSize(size.getWidth(), size.getHeight());
		parameters.setPreviewFormat(OldCameraHandler.IMAGE_FORMAT);
		
		/*//This might actually just decrease the FPS - recording hint takes care of this
		int[] fpsRange = parameters.getSupportedPreviewFpsRange().get(Camera.Parameters.PREVIEW_FPS_MIN_INDEX);
		Log.d("CMCM", "Camera FPS range: " + fpsRange[0] + " - " + fpsRange[1]);
		parameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);*/
	}
	
	private IntSize getBestSize(@NonNull List<IntSize> sizes) {
		int targetSize = 1280 * 720;
		IntSize bestSize = sizes.get(0);
		int bestDifference = Math.abs(targetSize - bestSize.getArea());
		for (int i = 1; i < sizes.size(); i++) {
			IntSize otherSize = sizes.get(i);
			int otherDifference = Math.abs(targetSize - otherSize.getArea());
			if (otherDifference < bestDifference) {
				bestSize = otherSize;
				bestDifference = otherDifference;
			}
		}
		return bestSize;
	}
}
