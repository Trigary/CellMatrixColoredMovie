package hu.trigary.cmcm.library.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A "regular-frame" type frame.
 */
final class ParsedRegularFrame extends ParsedFrame {
	private final int sequenceId;
	
	@Contract(pure = true)
	private ParsedRegularFrame(@NotNull byte[] dataBytes, int sequenceId) {
		super(dataBytes, 2);
		this.sequenceId = sequenceId;
	}
	
	/**
	 * Creates a new instance.
	 *
	 * @param dataBytes all the data stored in this frame, in raw form
	 * @return the newly created instance
	 */
	@NotNull
	@Contract(pure = true)
	public static ParsedRegularFrame create(@NotNull byte[] dataBytes) {
		int raw = readBytes(dataBytes, 0, 2);
		int sequenceId = raw >> 5;
		//int reserved = raw & 0x1f;
		return new ParsedRegularFrame(dataBytes, sequenceId);
	}
	
	/**
	 * Gets this frame's sequence ID inside the movie.
	 *
	 * @return this frame's sequence ID
	 */
	@Contract(pure = true)
	public int getSequenceId() {
		return sequenceId;
	}
	
	@Override
	@Contract(pure = true)
	public boolean equals(Object object) {
		return object instanceof ParsedRegularFrame && super.equals(object)
				&& ((ParsedRegularFrame) object).sequenceId == sequenceId;
	}
	
	@Override
	@Contract(pure = true)
	public int hashCode() {
		return super.hashCode() ^ sequenceId;
	}
}
