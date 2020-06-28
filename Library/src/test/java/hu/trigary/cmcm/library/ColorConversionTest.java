package hu.trigary.cmcm.library;

import hu.trigary.cmcm.library.utilities.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Unit test that makes sure colors are correctly converted to bits and the other way around.
 */
@Execution(ExecutionMode.CONCURRENT)
class ColorConversionTest {
	@Test
	void testColors() {
		for (Color color : Color.values()) {
			for (int x = 0; x < 128; x++) {
				for (int y = 0; y < 128; y++) {
					Assertions.assertSame(color, Color.fromBits(x, y, color.getBits(x, y)));
				}
			}
		}
	}
	
	@Test
	void testBits() {
		for (int bits = 0; bits < 8; bits++) {
			for (int x = 0; x < 128; x++) {
				for (int y = 0; y < 128; y++) {
					Assertions.assertEquals(bits, Color.fromBits(x, y, bits).getBits(x, y));
				}
			}
		}
	}
}
