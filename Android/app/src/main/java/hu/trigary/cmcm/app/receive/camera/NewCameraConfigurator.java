package hu.trigary.cmcm.app.receive.camera;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Range;
import android.util.Size;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NewCameraConfigurator {
	private final Range<Integer> fpsRange;
	private final Size size;
	
	public NewCameraConfigurator(@NonNull CameraCharacteristics camera) {
		StreamConfigurationMap config = camera.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		
		//noinspection ConstantConditions
		size = getBestSize(config.getOutputSizes(ImageFormat.YUV_420_888));
		
		Range<Integer>[] fpsRanges = camera.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
		if (fpsRanges != null && fpsRanges.length > 1) {
			Range<Integer> bestRange = fpsRanges[0];
			for (int i = 1; i < fpsRanges.length; i++) {
				Range<Integer> range = fpsRanges[i];
				if (range.getLower() * range.getUpper() > bestRange.getLower() * bestRange.getUpper()) {
					bestRange = range;
				}
			}
			
			fpsRange = bestRange;
		} else {
			fpsRange = null;
		}
		Log.d("CMCM", "Camera FPS range: " + fpsRange);
	}
	
	/**
	 * Configures the specified request.
	 *
	 * @param builder the request to configure
	 */
	public void configureRequest(@NonNull CaptureRequest.Builder builder) {
		builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
		builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
		builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
		builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
		builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF);
		builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
		builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
		builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
		builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST);
		builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_FAST);
		builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
		builder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_FAST);
		builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
		builder.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_FAST);
		builder.set(CaptureRequest.SENSOR_TEST_PATTERN_MODE, CaptureRequest.SENSOR_TEST_PATTERN_MODE_OFF);
		builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF);
		builder.set(CaptureRequest.STATISTICS_HOT_PIXEL_MAP_MODE, false);
		builder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE_OFF);
		
		if (fpsRange != null) {
			builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
		}
	}
	
	/**
	 * Gets the size of the images that the camera will provide.
	 *
	 * @return the images' size
	 */
	@NonNull
	public Size getSize() {
		return size;
	}
	
	private Size getBestSize(@NonNull Size[] sizes) {
		int targetSize = 1280 * 720;
		Size bestSize = sizes[0];
		int bestDifference = Math.abs(targetSize - bestSize.getHeight() * bestSize.getWidth());
		for (int i = 1; i < sizes.length; i++) {
			Size otherSize = sizes[i];
			int otherDifference = Math.abs(targetSize - otherSize.getHeight() * otherSize.getWidth());
			if (otherDifference < bestDifference) {
				bestSize = otherSize;
				bestDifference = otherDifference;
			}
		}
		return bestSize;
	}
}
