package hu.trigary.cmcm.library;

import hu.trigary.cmcm.library.utilities.ChunkConverter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Unit test that makes sure that bytes are correctly converted to chunks and back.
 */
@Execution(ExecutionMode.CONCURRENT)
class ChunkConverterTest {
	private static final int MIN_LENGTH = 1;
	private static final int MAX_LENGTH = 100;
	private static final int INDEX_OFFSET = Byte.MAX_VALUE / 10;
	private static final int RANDOM_ITERATIONS = 10000;
	
	@Test
	void testByteIndex() {
		for (int length = MIN_LENGTH; length <= MAX_LENGTH; length++) {
			byte[] bytes = new byte[length];
			for (int i = 0; i < length; i++) {
				bytes[i] = (byte) (INDEX_OFFSET + i);
			}
			test(bytes);
		}
	}
	
	@Test
	void testByteRandom() {
		for (int length = MIN_LENGTH; length <= MAX_LENGTH; length++) {
			byte[] bytes = new byte[length];
			for (int iteration = 0; iteration < RANDOM_ITERATIONS; iteration++) {
				ThreadLocalRandom.current().nextBytes(bytes);
				test(bytes);
			}
		}
	}
	
	private void test(@NotNull byte[] input) {
		int[] chunks = new int[ChunkConverter.getChunkCount(input.length)];
		ChunkConverter.bytesToChunks(input, chunks);
		byte[] bytes = new byte[ChunkConverter.getByteCount(chunks.length)];
		ChunkConverter.chunksToBytes(chunks, bytes);
		for (int i = 0; i < input.length; i++) {
			Assertions.assertEquals(bytes[i], input[i], () -> input.length
					+ " byte -> chunk -> byte failed, input bytes: " + Arrays.toString(input));
		}
	}
}
