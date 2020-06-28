package hu.trigary.cmcm.library.utilities;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The colors used to encode data.
 * Each color is able to hold 3 bits of information:
 * each color channel (red, green, blue) represents a bit.
 */
public enum Color {
	BLACK(false, false, false),
	RED(true, false, false),
	GREEN(false, true, false),
	BLUE(false, false, true),
	YELLOW(true, true, false),
	MAGENTA(true, false, true),
	AQUA(false, true, true),
	WHITE(true, true, true);
	
	private static final Color[] SORTED_VALUES = new Color[8];
	private final boolean red;
	private final boolean green;
	private final boolean blue;
	private final int argb;
	private final int bits;
	
	static {
		for (Color color : values()) {
			SORTED_VALUES[color.bits] = color;
		}
	}
	
	Color(boolean red, boolean green, boolean blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		argb = 0xff000000 | (red ? 0xff0000 : 0) | (green ? 0xff00 : 0) | (blue ? 0xff : 0);
		bits = (red ? 4 : 0) | (green ? 2 : 0) | (blue ? 1 : 0);
	}
	
	/**
	 * Gets whether this color has red in it.
	 *
	 * @return true if this color contains red
	 */
	@Contract(pure = true)
	public boolean hasRed() {
		return red;
	}
	
	/**
	 * Gets whether this color has green in it.
	 *
	 * @return true if this color contains green
	 */
	@Contract(pure = true)
	public boolean hasGreen() {
		return green;
	}
	
	/**
	 * Gets whether this color has blue in it.
	 *
	 * @return true if this color contains blue
	 */
	@Contract(pure = true)
	public boolean hasBlue() {
		return blue;
	}
	
	/**
	 * Gets this color in ARGB format.
	 * A is stored on the most significant bits and always has a value of 0xFF.
	 *
	 * @return this color in ARGB format
	 */
	@Contract(pure = true)
	public int getArgb() {
		return argb;
	}
	
	/**
	 * Gets the data represented by this color at the specified position.
	 *
	 * @param x the position's X coordinate
	 * @param y the position's Y coordinate
	 * @return the value this color at the specified position represents
	 */
	@Contract(pure = true)
	public int getBits(int x, int y) {
		return (bits + 8 - getMask(x, y)) % 8;
	}
	
	/**
	 * Gets the data represented by this color without the mask applied.
	 *
	 * @return the non-masked data represented by this color
	 */
	@Contract(pure = true)
	public int getBits() {
		return bits;
	}
	
	/**
	 * Gets the color that represents the data at the specified position.
	 *
	 * @param x the position's X coordinate
	 * @param y the position's Y coordinate
	 * @param bits the data to convert
	 * @return the color that the data at the specified position is represented by
	 */
	@NotNull
	@Contract(pure = true)
	public static Color fromBits(int x, int y, int bits) {
		return SORTED_VALUES[(bits + getMask(x, y)) % 8];
	}
	
	/**
	 * Gets the color that represents the data without the mask applied.
	 *
	 * @param bits the data to convert
	 * @return the color that the non-masked data is represented by
	 */
	@NotNull
	@Contract(pure = true)
	public static Color fromBits(int bits) {
		return SORTED_VALUES[bits];
	}
	
	/**
	 * Calculates the mask value at the specified position.
	 *
	 * @param x the position's X coordinate
	 * @param y the position's Y coordinate
	 * @return the calculated mask value
	 */
	@Contract(pure = true)
	public static int getMask(int x, int y) {
		return ((x * y) % (2 * x + y + 1) + y) % 8;
	}
}
