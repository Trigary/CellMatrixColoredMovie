package hu.trigary.cmcm.library.parser;

import hu.trigary.cmcm.library.utilities.Color;
import hu.trigary.cmcm.library.utilities.ChunkConverter;
import hu.trigary.cmcm.library.utilities.FrameInfo;
import hu.trigary.cmcm.library.utilities.reedsolomon.ReedSolomonDecoder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.zip.CRC32;

/**
 * The class that is used for converting an unordered stream of frames
 * (in the form of color matrices or content chunks) to the actual payload.
 */
public class MovieParser {
	private static final int EXPECTED_DUPLICATE_FRAMES_COUNT = 5;
	private final Map<Integer, List<ParsedRegularFrame>> regularFrames = new HashMap<>();
	private final List<ParsedFirstFrame> firstFrames = new ArrayList<>(EXPECTED_DUPLICATE_FRAMES_COUNT);
	private final FrameInfo frameInfo;
	private final byte[] payloadBytes;
	private final int contentChunksLength;
	private int[] contentChunks;
	
	/**
	 * Creates a new instance.
	 *
	 * @param frameInfo the properties of the frame that this instance will receive
	 */
	public MovieParser(@NotNull FrameInfo frameInfo) {
		this.frameInfo = frameInfo;
		payloadBytes = new byte[ChunkConverter.getByteCount(frameInfo.getDataChunks())];
		contentChunksLength = frameInfo.getContentChunks();
	}
	
	/**
	 * Adds a frame in the form of content chunks into this parser.
	 *
	 * @param contentChunks the frame to add
	 * @return the complete decoded payload, if decoding was successful
	 */
	@Nullable
	public byte[] tryAddFrame(@NotNull int[] contentChunks) {
		if (contentChunks.length != contentChunksLength) {
			throw new IllegalArgumentException("content chunks array length must equal FrameInfo#getContentChunks()");
		}
		
		return tryAddChunks(contentChunks);
	}
	
	/**
	 * Adds a frame in the form of a paddingless color matrix into this parser.
	 *
	 * @param paddinglessFrame the frame to add
	 * @return the complete decoded payload, if decoding was successful
	 */
	@Nullable
	public byte[] tryAddFrame(@NotNull Color[] paddinglessFrame) {
		int resolution = frameInfo.getContentResolution();
		if (paddinglessFrame.length != resolution * resolution) {
			throw new IllegalArgumentException("frame array length must equal squared resolution");
		}
		
		if (contentChunks == null) {
			contentChunks = new int[contentChunksLength];
		}
		
		int chunkIndex = 0;
		int firstChunk = paddinglessFrame[resolution].getBits(0, 0) << 9;
		firstChunk |= paddinglessFrame[1 + resolution].getBits(1, 0) << 6;
		firstChunk |= paddinglessFrame[2 * resolution - 1].getBits(0, 1) << 3;
		firstChunk |= paddinglessFrame[2 * resolution - 2].getBits(1, 1);
		contentChunks[chunkIndex++] = firstChunk;
		
		int chunkCount = resolution / 2;
		readChunksInRow(paddinglessFrame, 2, 0, resolution, chunkCount - 2, chunkIndex);
		int y = 2;
		chunkIndex += chunkCount - 2;
		for (int i = 2; i < chunkCount; i++) {
			readChunksInRow(paddinglessFrame, 0, y, resolution, chunkCount, chunkIndex);
			chunkIndex += chunkCount;
			y += 2;
		}
		readChunksInRow(paddinglessFrame, 1, y, resolution, chunkCount - 1, chunkIndex);
		
		return tryAddChunks(contentChunks);
	}
	
	private void readChunksInRow(@NotNull Color[] pixels, int startX, int y,
			int resolution, int chunkCount, int chunkIndex) {
		for (int i = 0; i < chunkCount; i++) {
			int value = pixels[startX + y * resolution].getBits(startX, y) << 9;
			value |= pixels[startX + 1 + y * resolution].getBits(startX + 1, y) << 6;
			value |= pixels[startX + (y + 1) * resolution].getBits(startX, y + 1) << 3;
			value |= pixels[startX + 1 + (y + 1) * resolution].getBits(startX + 1, y + 1);
			contentChunks[chunkIndex++] = value;
			startX += 2;
		}
	}
	
	@Nullable
	private byte[] tryAddChunks(@NotNull int[] contentChunks) {
		if (!ReedSolomonDecoder.decode(contentChunks, frameInfo.getCorrectionChunks())) {
			return null;
		}
		
		ChunkConverter.chunksToBytes(contentChunks, payloadBytes);
		if ((payloadBytes[0] & 128) != 0) {
			ParsedFirstFrame firstFrame = ParsedFirstFrame.create(payloadBytes, frameInfo);
			if (firstFrame == null) {
				return null;
			}
			
			synchronized (this) {
				storeFrame(firstFrames, firstFrame);
				return checkCombinations(firstFrame, null);
			}
		}
		
		ParsedRegularFrame regularFrame = ParsedRegularFrame.create(payloadBytes);
		synchronized (this) {
			List<ParsedRegularFrame> storage = regularFrames.get(regularFrame.getSequenceId());
			//noinspection Java8MapApi
			if (storage == null) {
				storage = new ArrayList<>(EXPECTED_DUPLICATE_FRAMES_COUNT);
				regularFrames.put(regularFrame.getSequenceId(), storage);
			}
			storeFrame(storage, regularFrame);
			
			//TODO sort first frames based on equalCount
			// and possibly based on the equality of their other fields
			// problem: probably a waste of CPU time
			
			//noinspection Convert2streamapi
			for (ParsedFirstFrame firstFrame : firstFrames) {
				byte[] result = checkCombinations(firstFrame, regularFrame);
				if (result != null) {
					return result;
				}
			}
			return null;
		}
	}
	
	private <E extends ParsedFrame> void storeFrame(@NotNull List<E> list, E frame) {
		for (int i = 0; i < list.size(); i++) {
			ParsedFrame current = list.get(i);
			if (current.equals(frame)) {
				frame.setEqualCount(current.getEqualCount() + 1);
				list.set(i, frame); //overwrite it, mitigates hash collision
				return;
			}
		}
		
		frame.setEqualCount(0);
		list.add(frame);
		
		//TODO should I try to eliminate frames which have very low equal count compared to this new frame? eg. 1/10
		// when to do this?
		// store the sum of the equal counts and remove all frames which are less than 10% of it or something like that
		// problem: if misread once, I will probably misread it again -> the minority can still be correct
	}
	
	@Nullable
	@Contract(pure = true)
	private byte[] checkCombinations(@NotNull ParsedFirstFrame firstFrame, @Nullable ParsedRegularFrame regularFrame) {
		if (regularFrame != null && regularFrame.getSequenceId() + 2 > firstFrame.getFrameCount()) {
			return null;
		}
		
		int excessFrames = regularFrames.size() + 1 - firstFrame.getFrameCount();
		if (excessFrames < 0) {
			return null;
		}
		
		int maxId = firstFrame.getFrameCount() - 2;
		for (int key : regularFrames.keySet()) {
			if (key > maxId) {
				excessFrames--;
				if (excessFrames < 0) {
					return null;
				}
			}
		}
		
		//noinspection unchecked
		List<? extends ParsedFrame>[] frames = new List[firstFrame.getFrameCount()];
		frames[0] = Collections.singletonList(firstFrame);
		for (Map.Entry<Integer, List<ParsedRegularFrame>> entry : regularFrames.entrySet()) {
			if (entry.getKey() <= maxId) {
				frames[entry.getKey() + 1] = entry.getValue();
			}
		}
		if (regularFrame != null) {
			frames[regularFrame.getSequenceId() + 1] = Collections.singletonList(regularFrame);
		}
		
		//noinspection Convert2streamapi
		for (ParsedFrame[] array : getPermutations(frames)) {
			if (crcMatches(firstFrame, array)) {
				return compilePayload(firstFrame, array);
			}
		}
		return null;
	}
	
	//[(1,2), (3,4), 5] -> [(1,3,5), (1,4,5), (2,3,5), (2,4,5)]
	@NotNull
	@Contract(pure = true)
	private ParsedFrame[][] getPermutations(@NotNull List<? extends ParsedFrame>[] input) {
		int size = 1;
		//noinspection Convert2streamapi
		for (List<? extends ParsedFrame> list : input) {
			size *= list.size();
		}
		ParsedFrame[][] output = new ParsedFrame[size][];
		int[] indexes = new int[input.length];
		
		for (int outer = 0; outer < output.length; outer++) {
			ParsedFrame[] array = new ParsedFrame[input.length];
			//noinspection Convert2streamapi
			for (int i = 0; i < array.length; i++) {
				array[i] = input[i].get(indexes[i]);
			}
			output[outer] = array;
			
			for (int i = input.length - 1; i >= 0; i--) {
				if (indexes[i] + 1 < input[i].size()) {
					indexes[i]++;
					break;
				}
				indexes[i] = 0;
			}
		}
		return output;
	}
	
	@Contract(pure = true)
	private static boolean crcMatches(@NotNull ParsedFirstFrame firstFrame, @NotNull ParsedFrame[] allFrames) {
		CRC32 crc = new CRC32();
		int remaining = firstFrame.getTotalPayloadLength();
		for (ParsedFrame frame : allFrames) {
			byte[] payload = frame.getPayload();
			int length = Math.min(payload.length, remaining);
			crc.update(payload, 0, length);
			remaining -= length;
		}
		return firstFrame.getTotalPayloadCrc() == (int) crc.getValue();
	}
	
	@NotNull
	@Contract(pure = true)
	private static byte[] compilePayload(@NotNull ParsedFirstFrame firstFrame, @NotNull ParsedFrame[] allFrames) {
		byte[] result = new byte[firstFrame.getTotalPayloadLength()];
		int remaining = result.length;
		for (ParsedFrame frame : allFrames) {
			byte[] payload = frame.getPayload();
			int length = Math.min(payload.length, remaining);
			System.arraycopy(payload, 0, result, result.length - remaining, length);
			remaining -= length;
		}
		return result;
	}
}
