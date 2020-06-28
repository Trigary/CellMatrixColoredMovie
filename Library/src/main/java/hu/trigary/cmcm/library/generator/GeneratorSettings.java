package hu.trigary.cmcm.library.generator;

import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Container of all options that can be set when creating a movie.
 */
public class GeneratorSettings extends FrameInfo {
	private final Padding padding;
	
	/**
	 * Creates a new instance.
	 *
	 * @param resolution the resolution to use
	 * @param padding the padding configuration to use
	 */
	public GeneratorSettings(@NotNull ContentResolution resolution, @NotNull Padding padding) {
		super(resolution);
		this.padding = padding;
	}
	
	/**
	 * Gets the padding type.
	 *
	 * @return the padding type
	 */
	@NotNull
	@Contract(pure = true)
	public Padding getPadding() {
		return padding;
	}
	
	/**
	 * Gets the image resolution, which is the padding resolution and the padding combined.
	 *
	 * @return the image resolution
	 */
	@Contract(pure = true)
	public int getImageResolution() {
		return getContentResolution() + 2 * padding.getWidth();
	}
	
	/**
	 * The padding types a frame can have.
	 */
	public enum Padding {
		/**
		 * Neither the black, nor the white border is added.
		 */
		NONE,
		
		/**
		 * Only the black border is added, the white isn't.
		 */
		BLACK_ONLY,
		
		/**
		 * Both the black and the white borders are added.
		 */
		BOTH;
		
		/**
		 * Gets the width this padding configuration adds to each side of the image.
		 *
		 * @return the width this padding adds to each side
		 */
		@Contract(pure = true)
		public int getWidth() {
			return this == NONE ? 0 : this == BLACK_ONLY ? 1 : 2;
		}
		
		/**
		 * Gets whether this padding configuration contains a white border.
		 *
		 * @return whether a white border exists
		 */
		@Contract(pure = true)
		public boolean hasWhitePadding() {
			return this == BOTH;
		}
		
		/**
		 * Gets whether this padding configuration contains a black border.
		 *
		 * @return whether a black border exists
		 */
		@Contract(pure = true)
		public boolean hasBlackPadding() {
			return this != NONE;
		}
	}
}
