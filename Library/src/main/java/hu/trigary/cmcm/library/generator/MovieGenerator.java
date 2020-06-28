package hu.trigary.cmcm.library.generator;

import hu.trigary.cmcm.library.utilities.Color;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * The class that is used for converting payloads in the form of bytes to movies
 * (sequence of frames, aka systems of color matrices).
 */
public final class MovieGenerator {
	
	private MovieGenerator() { }
	
	/**
	 * Converts the specified payload into a movie.
	 * {@link GeneratorSettings#getFrameCount(int)} should be called before this method
	 * to make sure that the parameters form a valid combination, aka the movie can be created.
	 *
	 * @param settings the properties of the frame that will be used
	 * @param payload the data that should be included in the movie
	 * @return the movie in the form of a sequence of frames (color matrices)
	 */
	@NotNull
	@Contract(pure = true)
	public static List<Color[]> createMovie(@NotNull GeneratorSettings settings, @NotNull byte[] payload) {
		int frameCount = settings.getFrameCount(payload.length);
		if (frameCount == -1) {
			throw new IllegalArgumentException("invalid payload length, settings combination");
		}
		
		PayloadQueue queue = new PayloadQueue(settings, payload);
		List<Callable<Color[]>> tasks = new ArrayList<>(frameCount);
		tasks.add(FrameGenerator.createFirstFrame(settings, queue));
		for (int i = 1; i < frameCount; i++) {
			tasks.add(FrameGenerator.createRegularFrame(settings, queue, i - 1));
		}
		
		try {
			List<Future<Color[]>> results = ForkJoinPool.commonPool().invokeAll(tasks);
			Color[][] tempFrames = new Color[frameCount][];
			for (int i = 0; i < results.size(); i++) {
				tempFrames[i] = results.get(i).get();
			}
			return Arrays.asList(tempFrames);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
