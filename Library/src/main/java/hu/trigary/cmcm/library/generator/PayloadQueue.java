package hu.trigary.cmcm.library.generator;

import hu.trigary.cmcm.library.utilities.ChunkConverter;
import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.zip.CRC32;

/**
 * A class that is capable of splitting the whole payload into smaller
 * segments segments that the frames then can then store.
 */
class PayloadQueue {
	private final FrameInfo frameInfo;
	private final byte[] payload;
	private final byte[] mutableDataBytesArray;
	private int mutableDataBytesArrayOffset;
	private int payloadIndex;
	
	/**
	 * Creates a new instance.
	 * The specified payload is not copied and therefore it mustn't be modified
	 * while this newly created instance is used.
	 *
	 * @param frameInfo the properties of the frame that will be used
	 * @param payload the data to include in the frame(s), not copied
	 */
	PayloadQueue(@NotNull FrameInfo frameInfo, @NotNull byte[] payload) {
		this.frameInfo = frameInfo;
		this.payload = payload;
		mutableDataBytesArray = new byte[ChunkConverter.getByteCount(frameInfo.getDataChunks())];
	}
	
	/**
	 * Gets the total length of the payload that is to be included in the frame(s).
	 *
	 * @return the payload's length
	 */
	@Contract(pure = true)
	public int getPayloadLength() {
		return payload.length;
	}
	
	/**
	 * Calculates a CRC32 value from the whole payload.
	 *
	 * @return the payload's CRC
	 */
	@Contract(pure = true)
	public int calculatePayloadCrc() {
		CRC32 crc = new CRC32();
		crc.update(payload, 0, payload.length);
		return (int) crc.getValue();
	}
	
	/**
	 * Sets the offset of the internal buffer and returns this buffer.
	 * The offset determines how many bytes at the start of the buffer won't be overwritten
	 * when {@link #createNextFrameQueue()} is called.
	 *
	 * @param offset the amount of bytes {@link #createNextFrameQueue()} should skip
	 * @return the internal, mutable buffer
	 */
	public byte[] setDataOffset(int offset) {
		mutableDataBytesArrayOffset = offset;
		return mutableDataBytesArray;
	}
	
	/**
	 * Creates a new {@link ColorQueue} from the data manually inserted into the internal buffer
	 * using {@link #setDataOffset(int)} and from the next payload segment.
	 * This newly created instance is not dependent on the this class instance
	 * and therefore can be given to another thread.
	 *
	 * @return a new {@link ColorQueue} instance
	 */
	@NotNull
	public ColorQueue createNextFrameQueue() {
		int length = mutableDataBytesArray.length - mutableDataBytesArrayOffset;
		if (payloadIndex + length <= payload.length) {
			System.arraycopy(payload, payloadIndex, mutableDataBytesArray, mutableDataBytesArrayOffset, length);
			payloadIndex += length;
		} else {
			int remaining = payload.length - payloadIndex;
			System.arraycopy(payload, payloadIndex, mutableDataBytesArray, mutableDataBytesArrayOffset, remaining);
			for (int i = mutableDataBytesArrayOffset + remaining; i < mutableDataBytesArray.length; i++) {
				mutableDataBytesArray[i] = 0;
			}
			payloadIndex = -1;
		}
		return new ColorQueue(frameInfo, mutableDataBytesArray);
	}
}
