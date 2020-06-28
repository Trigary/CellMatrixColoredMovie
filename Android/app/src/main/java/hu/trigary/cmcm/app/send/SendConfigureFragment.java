package hu.trigary.cmcm.app.send;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.SeekBar;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.util.FragmentBase;
import hu.trigary.cmcm.library.utilities.FrameInfo;

import java.util.Arrays;

/**
 * The fragment that lets the user configure the transmission details.
 */
public class SendConfigureFragment extends FragmentBase {
	private static final int DEFAULT_RESOLUTION_INDEX = Arrays.asList(FrameInfo.ContentResolution.values())
			.indexOf(FrameInfo.ContentResolution._16);
	private static final int[] FREQUENCY_VALUES = new int[30];
	private static final int DEFAULT_FREQUENCY_INDEX = 9;
	private byte[] payload;
	private int loadedResolutionIndex;
	private int loadedFrequencyIndex;
	
	static {
		for (int i = 0; i < FREQUENCY_VALUES.length; i++) {
			FREQUENCY_VALUES[i] = i + 1;
		}
	}
	
	public SendConfigureFragment() {
		super(R.layout.fragment_send_configure);
	}
	
	@NonNull
	public static SendConfigureFragment create(byte[] payload) {
		return FragmentBase.create(SendConfigureFragment.class, bundle -> {
			bundle.putByteArray("payload", payload);
			bundle.putInt("resolution", DEFAULT_RESOLUTION_INDEX);
			bundle.putInt("frequency", DEFAULT_FREQUENCY_INDEX);
		});
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			savedInstanceState = getArguments();
		}
		
		//noinspection ConstantConditions
		payload = savedInstanceState.getByteArray("payload");
		loadedResolutionIndex = savedInstanceState.getInt("resolution");
		loadedFrequencyIndex = savedInstanceState.getInt("frequency");
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putByteArray("payload", payload);
		SeekBar resolution = getView(R.id.send_configure_resolution_slider, SeekBar.class);
		if (resolution == null) {
			outState.putInt("resolution", loadedResolutionIndex);
			outState.putInt("frequency", loadedFrequencyIndex);
		} else {
			outState.putInt("resolution", resolution.getProgress());
			outState.putInt("frequency", getView(R.id.send_configure_frequency_slider, SeekBar.class).getProgress());
		}
	}
	
	@Override
	protected void initializeView() {
		initializeSeekBar(R.id.send_configure_resolution_slider, FrameInfo.ContentResolution.values().length - 1,
				loadedResolutionIndex, this::showValues);
		
		initializeSeekBar(R.id.send_configure_frequency_slider, FREQUENCY_VALUES.length - 1,
				loadedFrequencyIndex, this::showValues);
		
		addClickListener(R.id.send_configure_back, clicked -> goBack());
		addClickListener(R.id.send_configure_forward, clicked -> SendTransmittingFragment.create(payload,
				getResolution(), getFrequency()).display());
		
		showValues();
	}
	
	private void initializeSeekBar(int id, int max, int progress, Runnable onChange) {
		SeekBar seekBar = getView(id, SeekBar.class);
		seekBar.setMax(max);
		seekBar.setProgress(progress);
		seekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener(onChange));
	}
	
	private void showValues() {
		FrameInfo.ContentResolution resolution = getResolution();
		setText(R.id.send_configure_resolution_value, R.string.send_configure_resolution, resolution.getValue());
		int frameCount = new FrameInfo(resolution).getFrameCount(payload.length);
		
		setEnabled(R.id.send_configure_forward, frameCount != -1);
		if (frameCount == -1) {
			setText(R.id.send_configure_count, R.string.send_configure_count_impossible);
		} else {
			setText(R.id.send_configure_count, R.string.send_configure_count, frameCount);
		}
		
		setEnabled(R.id.send_configure_frequency_slider, frameCount != 1 && frameCount != -1);
		setText(R.id.send_configure_frequency_value, R.string.send_configure_frequency, getFrequency());
		
		if (frameCount == 1 || frameCount == -1) {
			setText(R.id.send_configure_time, R.string.send_configure_time_instant);
		} else {
			setText(R.id.send_configure_time, R.string.send_configure_time_normal,
					Math.ceil((double) frameCount / getFrequency() * 10) / 10);
		}
	}
	
	private FrameInfo.ContentResolution getResolution() {
		SeekBar seekBar = getView(R.id.send_configure_resolution_slider, SeekBar.class);
		return FrameInfo.ContentResolution.values()[seekBar.getProgress()];
	}
	
	private int getFrequency() {
		SeekBar seekBar = getView(R.id.send_configure_frequency_slider, SeekBar.class);
		return FREQUENCY_VALUES[seekBar.getProgress()];
	}
	
	private static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
		private final Runnable onChange;
		
		SimpleSeekBarListener(Runnable onChange) {
			this.onChange = onChange;
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			onChange.run();
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) { }
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) { }
	}
}
