package hu.trigary.cmcm.library;

import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;

/**
 * Unit test that makes sure that all supported resolutions have the required properties.
 */
@Execution(ExecutionMode.CONCURRENT)
class FrameInfoTest {
	@Test
	void testChunkDistribution() {
		Arrays.stream(FrameInfo.ContentResolution.values())
				.map(FrameInfo::new)
				.forEach(info -> {
					int content = info.getContentChunks();
					int correction = info.getCorrectionChunks();
					int data = info.getDataChunks();
					Assertions.assertEquals(content, correction + data, "sum of all chunks must equal content chunks");
					Assertions.assertEquals(0, data % 2, "count of data chunks must be even");
				});
	}
}
