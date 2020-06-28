package hu.trigary.cmcm.library;

import hu.trigary.cmcm.library.generator.GeneratorSettings;
import hu.trigary.cmcm.library.generator.MovieGenerator;
import hu.trigary.cmcm.library.parser.MovieParser;
import hu.trigary.cmcm.library.utilities.Color;
import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * Unit test that tests the whole system this project offers,
 * aka the {@link MovieGenerator} and {@link MovieParser} classes.
 * Corrupted chunks to test the Reed-Solomon error correction implementation are also created.
 */
@Execution(ExecutionMode.CONCURRENT)
class GeneratorParserTest {
	private static final FrameInfo.ContentResolution[] CONTENT_RESOLUTIONS = {FrameInfo.ContentResolution._8,
			FrameInfo.ContentResolution._12, FrameInfo.ContentResolution._16};
	private static final int MAX_PAYLOAD_LENGTH = 200;
	
	@RepeatedTest(20)
	void testNoErrors() {
		test((settings, payload) -> {
			List<Color[]> movie = MovieGenerator.createMovie(settings, payload);
			MovieParser parser = new MovieParser(settings);
			
			byte[] result = null;
			for (Color[] frame : movie) {
				Assertions.assertNull(result, "parsing must not complete before all frames are added");
				result = parser.tryAddFrame(removePadding(frame, settings.getPadding()));
			}
			
			Assertions.assertNotNull(result, "parsing must succeed after all frames are added");
			Assertions.assertArrayEquals(payload, result, "decoded payload must equal encoded payload");
		});
	}
	
	@RepeatedTest(20)
	void testCorrectableErrors() {
		test((settings, payload) -> {
			List<Color[]> movie = MovieGenerator.createMovie(settings, payload);
			MovieParser parser = new MovieParser(settings);
			
			byte[] result = null;
			for (Color[] frame : movie) {
				Assertions.assertNull(result, "parsing must not complete before all frames are added");
				frame = removePadding(frame, settings.getPadding());
				corruptPixels(frame, settings.getCorrectionChunks() / 2);
				result = parser.tryAddFrame(frame);
			}
			
			Assertions.assertNotNull(result, "parsing must succeed after all frames are added");
			Assertions.assertArrayEquals(payload, result, "decoded payload must equal encoded payload");
		});
	}
	
	@RepeatedTest(20)
	void testUncorrectableIncluded() {
		test((settings, payload) -> {
			List<Color[]> movie = MovieGenerator.createMovie(settings, payload);
			MovieParser parser = new MovieParser(settings);
			
			byte[] result = null;
			for (Color[] frame : movie) {
				Assertions.assertNull(result, "parsing must not complete before all frames are added");
				frame = removePadding(frame, settings.getPadding());
				corruptPixels(frame, settings.getDataChunks());
				result = parser.tryAddFrame(frame);
			}
			
			for (Color[] frame : movie) {
				if (result != null) {
					break;
				}
				result = parser.tryAddFrame(removePadding(frame, settings.getPadding()));
			}
			
			Assertions.assertNotNull(result, "parsing must succeed after all frames are added");
			Assertions.assertArrayEquals(payload, result, "decoded payload must equal encoded payload");
		});
	}
	
	@Contract(pure = true)
	private void test(@NotNull BiConsumer<GeneratorSettings, byte[]> consumer) {
		for (int payloadLength = 1; payloadLength <= MAX_PAYLOAD_LENGTH; payloadLength++) {
			byte[] payload = new byte[payloadLength];
			ThreadLocalRandom.current().nextBytes(payload);
			
			for (FrameInfo.ContentResolution resolution : CONTENT_RESOLUTIONS) {
				for (GeneratorSettings.Padding padding : GeneratorSettings.Padding.values()) {
					GeneratorSettings settings = new GeneratorSettings(resolution, padding);
					consumer.accept(settings, payload);
				}
			}
		}
	}
	
	private void corruptPixels(@NotNull Color[] frame, int maxErrorCount) {
		int[] indexes = IntStream.range(0, frame.length).toArray();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < indexes.length - 2; i++) {
			int j = random.nextInt(i, indexes.length);
			int temp = indexes[i];
			indexes[i] = indexes[j];
			indexes[j] = temp;
		}
		
		for (int i = 0; i < maxErrorCount; i++) {
			frame[indexes[i]] = Color.fromBits((frame[indexes[i]].getBits() + 1) % 8);
		}
	}
	
	@NotNull
	@Contract(pure = true)
	private Color[] removePadding(@NotNull Color[] frame, @NotNull GeneratorSettings.Padding padding) {
		if (padding == GeneratorSettings.Padding.NONE) {
			return Arrays.copyOf(frame, frame.length);
		}
		
		int paddingWidth = padding.getWidth();
		int oldResolution = (int) Math.sqrt(frame.length);
		int newResolution = oldResolution - 2 * paddingWidth;
		Color[] result = new Color[newResolution * newResolution];
		for (int x = 0; x < newResolution; x++) {
			for (int y = 0; y < newResolution; y++) {
				result[x + y * newResolution] = frame[x + paddingWidth + (y + paddingWidth) * oldResolution];
			}
		}
		return result;
	}
}
