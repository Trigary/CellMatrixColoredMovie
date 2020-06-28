package hu.trigary.cmcm.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

/**
 * The main (and only) activity in this application.
 */
public class MainActivity extends FragmentActivity {
	public static final boolean USE_TICKET_DEMO = false;
	public static final boolean DISPLAY_INSTEAD_OF_SEND = false;
	public static final boolean USE_NEW_CAMERA_API = true;
	public static final boolean USE_NATIVE_IMAGE_PROCESSOR = false; //TODO bug with native: crashes 2nd time I open RECEIVE
	//TODO native probably doesn't have all the features
	public static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
	private static MainActivity instance;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			MenuFragment.create().display();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}
	
	@NonNull
	public static MainActivity getInstance() {
		return instance;
	}
	
	@Override
	public void onBackPressed() {
		//don't pop the first fragment: that's the main menu
		if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
			getSupportFragmentManager().popBackStackImmediate();
		} else {
			finish();
		}
	}
}
