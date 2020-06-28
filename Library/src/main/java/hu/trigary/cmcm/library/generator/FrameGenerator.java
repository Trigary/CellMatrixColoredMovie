package hu.trigary.cmcm.library.generator;

import hu.trigary.cmcm.library.utilities.Color;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Utility class used for creating the different frames (color matrices) from a {@link PayloadQueue}.
 */
final class FrameGenerator {
	
	private FrameGenerator() { }
	
	/**
	 * Creates a new frame of the "first-frame" type.
	 *
	 * @param settings the properties of the frame that will be used
	 * @param queue the container of the payload that should be included in the frame
	 * @return a function that creates the requested frame and that can be called asynchronously
	 */
	@NotNull
	public static Callable<Color[]> createFirstFrame(@NotNull GeneratorSettings settings, @NotNull PayloadQueue queue) {
		byte[] bytes = queue.setDataOffset(7);
		setBytes(bytes, 0, queue.getPayloadLength(), 3);
		bytes[0] |= 128;
		setBytes(bytes, 3, queue.calculatePayloadCrc(), 4);
		return createFrame(settings, queue);
	}
	
	/**
	 * Creates a new frame of the "regular-frame" type.
	 *
	 * @param settings the properties of the frame that will be used
	 * @param queue the container of the payload that should be included in the frame
	 * @param sequenceId the sequence ID of this regular frame in the whole movie
	 * @return a function that creates the requested frame and that can be called asynchronously
	 */
	@NotNull
	public static Callable<Color[]> createRegularFrame(@NotNull GeneratorSettings settings,
			@NotNull PayloadQueue queue, int sequenceId) {
		byte[] bytes = queue.setDataOffset(2);
		setBytes(bytes, 0, sequenceId << 5, 2);
		//bytes[2] |= reserved;
		return createFrame(settings, queue);
	}
	
	private static void setBytes(@NotNull byte[] array, int offset, int value, int size) {
		for (int i = size - 1; i >= 0; i--) {
			array[offset + i] = (byte) value;
			value >>>= 8;
		}
	}
	
	@NotNull
	private static Callable<Color[]> createFrame(@NotNull GeneratorSettings settings, @NotNull PayloadQueue queue) {
		ColorQueue colorQueue = queue.createNextFrameQueue();
		return () -> {
			colorQueue.initialize();
			return createFrame(settings, colorQueue);
		};
	}
	
	@NotNull
	private static Color[] createFrame(@NotNull GeneratorSettings settings, @NotNull ColorQueue queue) {
		int resolution = settings.getImageResolution();
		Color[] pixels = new Color[resolution * resolution];
		
		int offset = 0;
		GeneratorSettings.Padding padding = settings.getPadding();
		if (padding.hasWhitePadding()) {
			drawRectangle(pixels, offset++, resolution, Color.WHITE);
		}
		if (padding.hasBlackPadding()) {
			drawRectangle(pixels, offset++, resolution, Color.BLACK);
		}
		setTemplatePixels(pixels, offset, resolution);
		
		//the first chunk is the one around the upper template bits
		Color[] colors = queue.calculateNextColors(0, 0);
		pixels[offset + (offset + 1) * resolution] = colors[0];
		pixels[offset + 1 + (offset + 1) * resolution] = colors[1];
		pixels[resolution - offset - 1 + (offset + 1) * resolution] = colors[2];
		pixels[resolution - offset - 2 + (offset + 1) * resolution] = colors[3];
		
		int chunkCount = resolution / 2 - offset;
		int y = offset;
		setChunksInRow(pixels, queue, offset, offset + 2, y, resolution, chunkCount - 2);
		y += 2;
		for (int i = 2; i < chunkCount; i++) {
			setChunksInRow(pixels, queue, offset, offset, y, resolution, chunkCount);
			y += 2;
		}
		setChunksInRow(pixels, queue, offset, offset + 1, y, resolution, chunkCount - 1);
		return pixels;
	}
	
	private static void drawRectangle(@NotNull Color[] pixels, int offset, int resolution, Color color) {
		int max = resolution - offset;
		for (int x = offset; x < max; x++) {
			pixels[x + offset * resolution] = color;
			pixels[x + (max - 1) * resolution] = color;
		}
		max--;
		for (int y = offset + 1; y < max; y++) {
			pixels[offset + y * resolution] = color;
			pixels[max + y * resolution] = color;
		}
	}
	
	private static void setTemplatePixels(@NotNull Color[] pixels, int offset, int resolution) {
		int bound = resolution - offset - 1;
		pixels[offset + offset * resolution] = Color.WHITE;
		pixels[offset + 1 + offset * resolution] = Color.BLACK;
		pixels[bound + offset * resolution] = Color.GREEN;
		pixels[bound - 1 + offset * resolution] = Color.MAGENTA;
		pixels[offset + bound * resolution] = Color.AQUA;
		pixels[offset + (bound - 1) * resolution] = Color.RED;
		pixels[bound + bound * resolution] = Color.YELLOW;
		pixels[bound + (bound - 1) * resolution] = Color.BLUE;
	}
	
	private static void setChunksInRow(@NotNull Color[] pixels, @NotNull ColorQueue queue,
			int offset, int startX, int y, int resolution, int chunkCount) {
		for (int i = 0; i < chunkCount; i++) {
			Color[] colors = queue.calculateNextColors(startX - offset, y - offset);
			pixels[startX + y * resolution] = colors[0];
			pixels[startX + 1 + y * resolution] = colors[1];
			pixels[startX + (y + 1) * resolution] = colors[2];
			pixels[startX + 1 + (y + 1) * resolution] = colors[3];
			startX += 2;
		}
	}
}
