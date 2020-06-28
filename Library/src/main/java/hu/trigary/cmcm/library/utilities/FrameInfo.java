package hu.trigary.cmcm.library.utilities;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Container of all properties a resolution has.
 */
public class FrameInfo {
	private static final double CORRECTION_RATIO_CONSTANT = 3.333;
	private final int contentResolution;
	
	/**
	 * Creates a new instance.
	 *
	 * @param resolution the resolution to use
	 */
	public FrameInfo(@NotNull ContentResolution resolution) {
		contentResolution = resolution.getValue();
	}
	
	/**
	 * Gets the resolution of the content, which is the image resolution without the paddings.
	 *
	 * @return the content resolution
	 */
	@Contract(pure = true)
	public int getContentResolution() {
		return contentResolution;
	}
	
	/**
	 * Gets the count of content chunks in a frame.
	 * A content chunk is either a correction or a data chunk.
	 *
	 * @return the count of content chunks in a frame
	 */
	@Contract(pure = true)
	public int getContentChunks() {
		return contentResolution * contentResolution / 4 - 2;
	}
	
	/**
	 * Gets the count of correction chunks in a frame.
	 *
	 * @return the count of correction chunks in a frame
	 */
	@Contract(pure = true)
	public int getCorrectionChunks() {
		int content = getContentChunks();
		int correction = (int) (content / CORRECTION_RATIO_CONSTANT);
		return (content - correction) % 2 == 0 ? correction : correction + 1;
	}
	
	/**
	 * Gets the count of data chunks in a frame.
	 *
	 * @return the count of data chunks in a frame
	 */
	@Contract(pure = true)
	public int getDataChunks() {
		int content = getContentChunks();
		int data = content - (int) (content / CORRECTION_RATIO_CONSTANT);
		return data % 2 == 0 ? data : data - 1;
	}
	
	/**
	 * Calculates the amount of frames necessary to contain a payload of the specified length.
	 * Returns -1 if the payload length is invalid or is too large.
	 *
	 * @param payloadLength the length of the payload
	 * @return the amount of frames necessary or -1
	 */
	@Contract(pure = true)
	public int getFrameCount(int payloadLength) {
		if (payloadLength <= 0) {
			return -1;
		}
		
		int dataBits = getDataChunks() * 12;
		int payloadBitsRegular = dataBits - 16;
		int count = (payloadLength * 8 - dataBits + 63 + payloadBitsRegular) / payloadBitsRegular;
		return count > 1024 ? -1 : count + 1;
	}
	
	
	
	/**
	 * The different resolutions frames can have.
	 */
	public enum ContentResolution {
		_8,
		_12,
		_16,
		_24,
		_32,
		_40,
		_50,
		_64,
		_80,
		_100,
		_128;
		
		private final int value;
		
		ContentResolution() {
			value = Integer.parseInt(name().substring(1));
		}
		
		/**
		 * The resolution itself, as a number.
		 *
		 * @return the resolution
		 */
		@Contract(pure = true)
		public int getValue() {
			return value;
		}
		
		/**
		 * Gets the instance associated with the specified resolution.
		 * Returns null if no instances are associated with the specified value.
		 *
		 * @param resolution the resolution to search for
		 * @return the instance associated with the specified resolution or null
		 */
		@Nullable
		public static ContentResolution fromValue(int resolution) {
			//noinspection Convert2streamapi
			for (ContentResolution value : values()) {
				if (value.getValue() == resolution) {
					return value;
				}
			}
			return null;
		}
	}
}
