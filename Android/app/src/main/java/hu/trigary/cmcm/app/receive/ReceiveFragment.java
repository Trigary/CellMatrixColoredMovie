package hu.trigary.cmcm.app.receive;

import android.support.annotation.NonNull;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.receive.camera.DisplayCameraHandler;
import hu.trigary.cmcm.app.util.FragmentBase;

/**
 * The fragment that lets the user decode movies.
 */
public class ReceiveFragment extends FragmentBase implements SurfaceHolder.Callback {
	private DisplayCameraHandler cameraHandler;
	
	static {
		System.loadLibrary("opencv_java4");
		System.loadLibrary("native-lib");
	}
	
	public ReceiveFragment() {
		super(R.layout.fragment_receive);
	}
	
	@NonNull
	public static ReceiveFragment create() {
		return new ReceiveFragment();
	}
	
	@Override
	public void initializeView() {
		//noinspection ConstantConditions
		cameraHandler = DisplayCameraHandler.create(getContext());
		getView(R.id.receive_preview, SurfaceView.class).getHolder().addCallback(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		getView(R.id.receive_preview).setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getView(R.id.receive_preview).setVisibility(View.GONE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		cameraHandler.onDestroyed();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//noinspection ConstantConditions
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		cameraHandler.onSurfaceCreated(holder);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//noinspection ConstantConditions
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		cameraHandler.onSurfaceDestroyed();
	}
}
