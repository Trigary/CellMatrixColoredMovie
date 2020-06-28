package hu.trigary.cmcm.app.util;

import android.support.annotation.NonNull;
import hu.trigary.cmcm.app.MainActivity;

/**
 * Utility class that allows a callback to be repeatedly called until manually stopped.
 */
public class RepeatingTask {
	private final int delayMillis;
	private final Runnable scheduledAction;
	
	/**
	 * Creates a new instance.
	 *
	 * @param delayMillis the delay between subsequent calls
	 * @param action the callback to execute
	 */
	public RepeatingTask(int delayMillis, @NonNull Runnable action) {
		this.delayMillis = delayMillis;
		scheduledAction = () -> {
			start();
			action.run();
		};
	}
	
	/**
	 * Start repeatedly calling the callback.
	 */
	public void start() {
		MainActivity.MAIN_HANDLER.postDelayed(scheduledAction, delayMillis);
	}
	
	/**
	 * Stop repeatedly calling the callback.
	 */
	public void stop() {
		MainActivity.MAIN_HANDLER.removeCallbacks(scheduledAction);
	}
}
