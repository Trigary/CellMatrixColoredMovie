package hu.trigary.cmcm.app.send;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.util.FragmentBase;
import hu.trigary.cmcm.app.util.RepeatingTask;
import hu.trigary.cmcm.library.generator.GeneratorSettings;
import hu.trigary.cmcm.library.generator.MovieGenerator;
import hu.trigary.cmcm.library.utilities.Color;
import hu.trigary.cmcm.library.utilities.FrameInfo;

import java.util.List;

/**
 * The fragment that display the movie other phones can decode.
 */
public class SendTransmittingFragment extends FragmentBase {
	private Bundle instanceState;
	private boolean waitForDisplay;
	private MovieGeneratorTask generatorTask;
	private int deltaMillis;
	private RepeatingTask nextFrameTask;
	private List<Color[]> rawFrames;
	private int frameIndex;
	private boolean sending;
	
	public SendTransmittingFragment() {
		super(R.layout.fragment_send_transmitting);
	}
	
	@NonNull
	public static SendTransmittingFragment create(byte[] payload, FrameInfo.ContentResolution resolution, int frequency) {
		return FragmentBase.create(SendTransmittingFragment.class, bundle -> {
			bundle.putByteArray("payload", payload);
			bundle.putSerializable("resolution", resolution);
			bundle.putInt("frequency", frequency);
		});
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instanceState = savedInstanceState == null ? getArguments() : savedInstanceState;
		
		waitForDisplay = true;
		generatorTask = new MovieGeneratorTask();
		generatorTask.execute(instanceState.get("resolution"), instanceState.getByteArray("payload"));
		
		deltaMillis = Math.round(1000f / instanceState.getInt("frequency"));
		nextFrameTask = new RepeatingTask(deltaMillis, this::showNextFrame);
		
		frameIndex = 0;
		sending = false;
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putAll(instanceState);
	}
	
	@Override
	protected void initializeView() {
		addClickListener(R.id.send_transmitting_toggle, clicked -> {
			if (sending) {
				stopSending();
			} else {
				startSending();
			}
		});
		
		addClickListener(R.id.send_transmitting_back, clicked -> {
			if (sending) {
				nextFrameTask.stop();
			}
			goBack();
		});
		
		getView(R.id.send_transmitting_progress_bar, ProgressBar.class).setProgress(0);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		waitForDisplay = false;
		
		if (rawFrames != null) {
			setupDisplay();
		}
		if (rawFrames == null || rawFrames.size() == 1) {
			getView(R.id.send_transmitting_toggle).setVisibility(View.INVISIBLE);
			getView(R.id.send_transmitting_container).setVisibility(View.INVISIBLE);
		}
		
		//noinspection ConstantConditions
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		waitForDisplay = true;
		
		if (sending) {
			stopSending();
		}
		
		//noinspection ConstantConditions
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		generatorTask.cancel(true);
	}
	
	@SuppressLint("StaticFieldLeak")
	private class MovieGeneratorTask extends AsyncTask<Object, Void, List<Color[]>> {
		@Override
		protected List<Color[]> doInBackground(Object... params) {
			return MovieGenerator.createMovie(new GeneratorSettings((FrameInfo.ContentResolution) params[0],
					GeneratorSettings.Padding.NONE), (byte[]) params[1]);
		}
		
		@Override
		protected void onPostExecute(List<Color[]> result) {
			//noinspection AssignmentOrReturnOfFieldWithMutableType
			rawFrames = result;
			if (waitForDisplay) {
				return;
			}
			
			setupDisplay();
			if (rawFrames.size() != 1) {
				getView(R.id.send_transmitting_toggle).setVisibility(View.VISIBLE);
				getView(R.id.send_transmitting_container).setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void setupDisplay() {
		getView(R.id.send_transmitting_progress_bar, ProgressBar.class).setMax(rawFrames.size() - 1);
		showProgress();
		
		getView(R.id.send_transmitting_loading).setVisibility(View.GONE);
		ImageView frame = getView(R.id.send_transmitting_frame, ImageView.class);
		frame.setVisibility(View.VISIBLE);
		frame.post(this::showCurrentFrame);
	}
	
	private void startSending() {
		sending = true;
		setText(R.id.send_transmitting_toggle, R.string.send_transmitting_pause);
		nextFrameTask.start();
	}
	
	private void stopSending() {
		sending = false;
		setText(R.id.send_transmitting_toggle, R.string.send_transmitting_start);
		nextFrameTask.stop();
	}
	
	private void showProgress() {
		setText(R.id.send_transmitting_progress_value, R.string.send_transmitting_progress,
				Math.ceil((rawFrames.size() - frameIndex) * deltaMillis / 100f) / 10);
		getView(R.id.send_transmitting_progress_bar, ProgressBar.class)
				.setProgress(frameIndex);
	}
	
	private void showNextFrame() {
		frameIndex = (frameIndex + 1) % rawFrames.size();
		showProgress();
		showCurrentFrame();
	}
	
	private void showCurrentFrame() {
		ImageView container = getView(R.id.send_transmitting_frame, ImageView.class);
		int totalSize = container.getWidth();
		Color[] pixels = rawFrames.get(frameIndex);
		int resolution = (int) Math.sqrt(pixels.length);
		int pixelSize = totalSize / (resolution + 4);
		totalSize = (resolution + 2) * pixelSize;
		
		Bitmap bitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.BLACK.getArgb());
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		
		for (int x = 0; x < resolution; x++) {
			for (int y = 0; y < resolution; y++) {
				Color color = pixels[x + y * resolution];
				if (color == Color.BLACK) {
					continue;
				}
				
				paint.setColor(color.getArgb());
				canvas.drawRect((x + 1) * pixelSize, (y + 1) * pixelSize,
						(x + 2) * pixelSize, (y + 2) * pixelSize, paint);
			}
		}
		
		container.setImageBitmap(bitmap);
	}
}
