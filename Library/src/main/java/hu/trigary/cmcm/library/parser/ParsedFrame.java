package hu.trigary.cmcm.library.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.zip.CRC32;

/**
 * A common superclass for all frame types that can be decoded.
 */
class ParsedFrame {
	private final byte[] payload;
	private final int payloadCrc;
	private int equalCount = -1;
	
	@Contract(pure = true)
	protected ParsedFrame(@NotNull byte[] dataBytes, int payloadStartIndex) {
		payload = new byte[dataBytes.length - payloadStartIndex];
		System.arraycopy(dataBytes, payloadStartIndex, payload, 0, payload.length);
		CRC32 crc = new CRC32();
		crc.update(payload, 0, payload.length);
		payloadCrc = (int) crc.getValue();
	}
	
	@Contract(pure = true)
	protected static int readBytes(@NotNull byte[] bytes, int offset, int size) {
		int value = bytes[offset] & 0xff;
		for (int i = 1; i < size; i++) {
			value <<= 8;
			value |= bytes[offset + i] & 0xff;
		}
		return value;
	}
	
	/**
	 * Gets the actual payload this frame contains.
	 * The returned value is not cloned, therefore must not be mutated.
	 *
	 * @return this frame's payload
	 */
	@NotNull
	@Contract(pure = true)
	public byte[] getPayload() {
		//noinspection AssignmentOrReturnOfFieldWithMutableType
		return payload;
	}
	
	/**
	 * Sets the amount of frames that are seemingly identical to this one.
	 * Must be called exactly once for each instance.
	 *
	 * @param count the amount of identical frames
	 */
	public void setEqualCount(int count) {
		equalCount = count;
	}
	
	/**
	 * Gets the amount of frames identical to this one.
	 *
	 * @return the amount of identical frames
	 */
	@Contract(pure = true)
	public int getEqualCount() {
		return equalCount;
	}
	
	@Override
	@Contract(pure = true)
	public boolean equals(Object object) {
		if (!(object instanceof ParsedFrame)) {
			return false;
		}
		
		ParsedFrame other = (ParsedFrame) object;
		return other.payload.length == payload.length && other.payloadCrc == payloadCrc;
	}
	
	@Override
	@Contract(pure = true)
	public int hashCode() {
		return payloadCrc;
	}
}
