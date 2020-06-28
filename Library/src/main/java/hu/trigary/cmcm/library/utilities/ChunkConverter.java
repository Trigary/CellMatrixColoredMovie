package hu.trigary.cmcm.library.utilities;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class that can convert byte arrays to 12-bit chunk arrays and the other way around.
 */
public final class ChunkConverter {
	
	private ChunkConverter() { }
	
	/**
	 * Calculates the amount of chunks necessary to contain the specified amount of bytes.
	 *
	 * @param byteCount the amount of bytes
	 * @return the required amount of chunks or -1 if the parameter was invalid
	 */
	@Contract(pure = true)
	public static int getChunkCount(int byteCount) {
		return byteCount <= 0 ? -1 : (byteCount * 8 + 11) / 12;
	}
	
	/**
	 * Converts the specified bytes to chunks.
	 *
	 * @param input the container of the bytes
	 * @param output the array in which the calculated chunks should be stored
	 */
	public static void bytesToChunks(@NotNull byte[] input, @NotNull int[] output) {
		int inputIndex = 0;
		int remainder = input.length % 3;
		int inputIndexBound = input.length - remainder;
		int outputIndex = 0;
		
		while (inputIndex < inputIndexBound) {
			byte first = input[inputIndex++];
			byte second = input[inputIndex++];
			byte third = input[inputIndex++];
			output[outputIndex++] = ((first << 4) & 0xff0) | ((second >>> 4) & 0xf);
			output[outputIndex++] = ((second << 8) & 0xf00) | (third & 0xff);
		}
		
		if (remainder == 1) {
			byte first = input[inputIndex];
			output[outputIndex] = (first << 4) & 0xff0;
			return;
		}
		
		if (remainder == 2) {
			byte first = input[inputIndex++];
			byte second = input[inputIndex];
			output[outputIndex++] = ((first << 4) & 0xff0) | ((second >>> 4) & 0xf);
			output[outputIndex] = (second << 8) & 0xf00;
		}
	}
	
	/**
	 * Calculates the amount of bytes necessary to contain the specified amount of chunks.
	 *
	 * @param chunkCount the amount of chunks
	 * @return the required amount of bytes or -1 if the parameter was invalid
	 */
	@Contract(pure = true)
	public static int getByteCount(int chunkCount) {
		return chunkCount <= 0 ? -1 : chunkCount * 12 / 8;
	}
	
	/**
	 * Converts the specified chunks to bytes.
	 *
	 * @param input the container of the chunks
	 * @param output the array in which the calculated bytes should be stored
	 */
	public static void chunksToBytes(@NotNull int[] input, @NotNull byte[] output) {
		int inputIndex = 0;
		int outputIndex = 0;
		int remainder = output.length % 3;
		int outputIndexBound = output.length - remainder;
		
		while (outputIndex < outputIndexBound) {
			int first = input[inputIndex++];
			int second = input[inputIndex++];
			output[outputIndex++] = (byte) (first >>> 4);
			output[outputIndex++] = (byte) (((first << 4) & 0xf0) | (second >>> 8));
			output[outputIndex++] = (byte) second;
		}
		
		if (remainder == 1) {
			int first = input[inputIndex];
			output[outputIndex] = (byte) (first >>> 4);
			return;
		}
		
		if (remainder == 2) {
			int first = input[inputIndex++];
			int second = input[inputIndex];
			output[outputIndex++] = (byte) (first >>> 4);
			output[outputIndex] = (byte) (((first << 4) & 0xf0) | (second >>> 8));
		}
	}
}
