package hu.trigary.cmcm.app.util;

/**
 * An immutable pair of integers that represent the width and height of something.
 */
public final class IntSize {
	private final int width;
	private final int height;
	
	/**
	 * Creates a new instance.
	 *
	 * @param width the width value
	 * @param height the height value
	 */
	public IntSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Gets the width value.
	 *
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * Gets the height value.
	 *
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Gets the area, which is the width multiplied by the height.
	 *
	 * @return the area defined by the width, height values
	 */
	public int getArea() {
		return width * height;
	}
	
	@Override
	public String toString() {
		return width + "x" + height;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof IntSize)) {
			return false;
		}
		
		IntSize size = (IntSize) other;
		return width == size.width && height == size.height;
	}
	
	@Override
	public int hashCode() {
		return (width * 53) ^ height;
	}
}
