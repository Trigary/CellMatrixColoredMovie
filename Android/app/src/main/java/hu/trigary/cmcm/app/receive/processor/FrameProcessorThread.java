package hu.trigary.cmcm.app.receive.processor;

import android.os.Process;
import android.support.annotation.NonNull;
import hu.trigary.cmcm.app.MainActivity;

/**
 * A superclass for threads responsible for processing frames.
 */
public abstract class FrameProcessorThread extends Thread {
	protected final FrameProcessorManager manager;
	protected byte[] buffer;
	
	protected FrameProcessorThread(int id, FrameProcessorManager manager) {
		super("CMCM:FrameProcessor#" + id);
		this.manager = manager;
		Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
	}
	
	/**
	 * Creates a new instance.
	 *
	 * @param id the ID of the thread
	 * @param manager the manager instance
	 * @return the newly created thread
	 */
	@NonNull
	public static FrameProcessorThread create(int id, FrameProcessorManager manager) {
		return MainActivity.USE_NATIVE_IMAGE_PROCESSOR
				? new FrameProcessorThreadNative(id, manager)
				: new FrameProcessorThreadJava(id, manager);
	}
	
	/**
	 * Called when the a frame was received and should be processed.
	 *
	 * @param buffer the frame's container
	 */
	public final synchronized void onFrameReceived(@NonNull byte[] buffer) {
		this.buffer = buffer;
		notify();
	}
	
	/**
	 * Gets the frame buffer currently associated with this thread.
	 *
	 * @return the current frame buffer
	 */
	@NonNull
	public final synchronized byte[] getBuffer() {
		return buffer;
	}
}
