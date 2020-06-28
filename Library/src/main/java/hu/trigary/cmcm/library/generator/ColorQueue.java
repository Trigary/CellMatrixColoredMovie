package hu.trigary.cmcm.library.generator;

import hu.trigary.cmcm.library.utilities.ChunkConverter;
import hu.trigary.cmcm.library.utilities.Color;
import hu.trigary.cmcm.library.utilities.FrameInfo;
import hu.trigary.cmcm.library.utilities.reedsolomon.ReedSolomonEncoder;
import org.jetbrains.annotations.NotNull;

/**
 * A class that is capable of converting byte data to chunks in the form 4 {@link Color} references.
 */
class ColorQueue {
	private final Color[] colors = new Color[4];
	private final int[] dataChunks;
	private final int[] correctionChunks;
	private int[] values;
	private int index;
	
	/**
	 * Creates a new instance.
	 *
	 * @param frameInfo the properties of the frame that will be used
	 * @param dataBytes the data to include in the frame(s), copied internally
	 */
	ColorQueue(@NotNull FrameInfo frameInfo, @NotNull byte[] dataBytes) {
		dataChunks = new int[frameInfo.getDataChunks()];
		correctionChunks = new int[frameInfo.getCorrectionChunks()];
		ChunkConverter.bytesToChunks(dataBytes, dataChunks);
	}
	
	/**
	 * Initializes this instance, must be called before any
	 * {@link #calculateNextColors(int, int)} calls are made.
	 * This method is not called in the constructor in order to
	 * allow callers to take advantage of multithreading.
	 */
	public void initialize() {
		ReedSolomonEncoder.encode(dataChunks, correctionChunks);
		values = dataChunks;
	}
	
	/**
	 * Calculates the next 4 colors at the specified top-left position.
	 * The returned array is reused internally,
	 * its contents are overwritten whenever this method is called.
	 *
	 * @param x the position's X coordinate
	 * @param y the position's Y coordinate
	 * @return the 4 calculated colors
	 */
	@NotNull
	public Color[] calculateNextColors(int x, int y) {
		if (index == values.length) {
			values = correctionChunks;
			index = 0;
		}
		
		int value = values[index++];
		colors[0] = Color.fromBits(x, y, value >>> 9);
		colors[1] = Color.fromBits(x + 1, y, (value >>> 6) & 7);
		colors[2] = Color.fromBits(x, y + 1, (value >>> 3) & 7);
		colors[3] = Color.fromBits(x + 1, y + 1, value & 7);
		return colors;
	}
}
