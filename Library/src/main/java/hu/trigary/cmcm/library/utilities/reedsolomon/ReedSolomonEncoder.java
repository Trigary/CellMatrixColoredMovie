/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified by the CMCM author(s).
 * Its original form can be found at: https://github.com/zxing/zxing
 * The license and copyright notice in this header does not apply to these modifications.
 */

package hu.trigary.cmcm.library.utilities.reedsolomon;

import hu.trigary.cmcm.library.utilities.FrameInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements Reed-Solomon encoding: given an array containing the payload chunks,
 * it tries to generate the correction chunks for it.
 */
public final class ReedSolomonEncoder {
	private static final Map<Integer, GaloisFieldPoly> CACHED_GENERATORS = new HashMap<>();
	
	static {
		FrameInfo.ContentResolution[] resolutions = FrameInfo.ContentResolution.values();
		int[] correctionChunkLengths = new int[resolutions.length];
		//noinspection Convert2streamapi
		for (int i = 0; i < resolutions.length; i++) {
			correctionChunkLengths[i] = new FrameInfo(resolutions[i]).getCorrectionChunks();
		}
		
		int index = 0;
		int degree = 0;
		GaloisFieldPoly generator = new GaloisFieldPoly(new int[]{1});
		while (index < correctionChunkLengths.length) {
			generator = generator.multiply(new GaloisFieldPoly(new int[]{1, GaloisField.exp(++degree)}));
			if (degree == correctionChunkLengths[index]) {
				index++;
				CACHED_GENERATORS.put(degree, generator);
			}
		}
	}
	
	private ReedSolomonEncoder() { }
	
	/**
	 * Generates error correction values into the specified array based on the specified payload.
	 * The elements of both arrays are chunks. Each chunk's value is at least 0 and less than {@link GaloisField#SIZE}.
	 *
	 * @param payload the chunks to encrypt
	 * @param corrections the array to fill with the correction chunks (all elements are overwritten internally)
	 */
	public static void encode(@NotNull int[] payload, @NotNull int[] corrections) {
		if (payload.length <= 0) {
			throw new IllegalArgumentException("payload mustn't be empty");
		} else if (corrections.length < 2) {
			throw new IllegalArgumentException("must be at least two error correction chunks");
		} else if (payload.length + corrections.length >= GaloisField.SIZE) {
			throw new IllegalArgumentException("block size limit mustn't be crossed");
		}
		
		int[] coefficients = new GaloisFieldPoly(payload)
				.multiplyByMonomial(corrections.length, 1)
				.divide(CACHED_GENERATORS.get(corrections.length))[1].getCoefficients();
		
		int zeroCount = corrections.length - coefficients.length;
		for (int i = 0; i < zeroCount; i++) {
			corrections[i] = 0; //clear only that part of the array which is not overwritten
		}
		
		//
		System.arraycopy(coefficients, 0, corrections, zeroCount, coefficients.length);
	}
}
