package hu.trigary.cmcm.library.parser;

import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A "first-frame" type frame.
 */
final class ParsedFirstFrame extends ParsedFrame {
	private final int totalPayloadLength;
	private final int totalPayloadCrc;
	private final int frameCount;
	
	@Contract(pure = true)
	private ParsedFirstFrame(@NotNull byte[] dataBytes, int totalPayloadLength, int totalPayloadCrc, int frameCount) {
		super(dataBytes, 7);
		this.totalPayloadLength = totalPayloadLength;
		this.totalPayloadCrc = totalPayloadCrc;
		this.frameCount = frameCount;
	}
	
	/**
	 * Attempts to create a new instance.
	 * Null is returned in case it is clear that the data is corrupted, invalid.
	 *
	 * @param dataBytes all the data stored in this frame, in raw form
	 * @param frameInfo the properties of this frame
	 * @return the newly created instance or null
	 */
	@Nullable
	@Contract(pure = true)
	public static ParsedFirstFrame create(@NotNull byte[] dataBytes, @NotNull FrameInfo frameInfo) {
		int totalPayloadLength = readBytes(dataBytes, 0, 3) & 0x7fffff;
		int frameCount = frameInfo.getFrameCount(totalPayloadLength);
		if (frameCount == -1) {
			return null;
		}
		
		int totalPayloadCrc = readBytes(dataBytes, 3, 4);
		return new ParsedFirstFrame(dataBytes, totalPayloadLength, totalPayloadCrc, frameCount);
	}
	
	/**
	 * Gets the total length of the payload in the movie.
	 *
	 * @return the movie payload's total length
	 */
	@Contract(pure = true)
	public int getTotalPayloadLength() {
		return totalPayloadLength;
	}
	
	/**
	 * Gets the CRC of the total movie payload.
	 *
	 * @return the movie payload's CRC
	 */
	@Contract(pure = true)
	public int getTotalPayloadCrc() {
		return totalPayloadCrc;
	}
	
	/**
	 * Gets the amount of frames in the movie.
	 *
	 * @return the amount of frames in the movie
	 */
	@Contract(pure = true)
	public int getFrameCount() {
		return frameCount;
	}
	
	@Override
	@Contract(pure = true)
	public boolean equals(Object object) {
		if (!(object instanceof ParsedFirstFrame) || !super.equals(object)) {
			return false;
		}
		
		ParsedFirstFrame other = (ParsedFirstFrame) object;
		return other.totalPayloadLength == totalPayloadLength && other.totalPayloadCrc == totalPayloadCrc;
	}
	
	@Override
	@Contract(pure = true)
	public int hashCode() {
		return super.hashCode() ^ totalPayloadLength ^ totalPayloadCrc;
	}
}
