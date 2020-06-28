/*
 * Copyright 2007 ZXing authors
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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implements Reed-Solomon decoding: given an array containing the payload chunks and the error correction chunks,
 * it tries to decode and error correct these.
 */
public final class ReedSolomonDecoder {
	
	private ReedSolomonDecoder() { }
	
	/**
	 * Attempts to detect and correct all errors in the payload chunks using the error correction chunks.
	 * The correct values are written back into the array.
	 *
	 * @param chunks the array containing both the payload and the error correction chunks
	 * @param corrections the count of error correction chunks
	 * @return true if the decoding was successful, false if not all errors could be corrected
	 */
	public static boolean decode(@NotNull int[] chunks, int corrections) {
		if (chunks.length <= corrections) {
			throw new IllegalArgumentException("must be more total chunks than error correction chunks");
		} else if (corrections < 2) {
			throw new IllegalArgumentException("must be at least two error correction chunks");
		} else if (chunks.length >= GaloisField.SIZE) {
			throw new IllegalArgumentException("block size limit mustn't be crossed");
		}
		
		GaloisFieldPoly poly = new GaloisFieldPoly(chunks);
		int[] syndromeCoefficients = new int[corrections];
		
		boolean noError = true;
		for (int i = 1; i <= corrections; i++) {
			int eval = poly.evaluateAt(GaloisField.exp(i));
			syndromeCoefficients[syndromeCoefficients.length - i] = eval;
			//noinspection IfStatementMissingBreakInLoop - IntelliJ is being stupid
			if (eval != 0) {
				noError = false;
			}
		}
		if (noError) {
			return true;
		}
		
		GaloisFieldPoly[] sigmaOmega = runEuclideanAlgorithm(GaloisField.buildMonomial(corrections, 1),
				new GaloisFieldPoly(syndromeCoefficients), corrections);
		if (sigmaOmega == null) {
			return false;
		}
		
		int[] errorLocations = findErrorLocations(sigmaOmega[0]);
		if (errorLocations == null) {
			return false;
		}
		
		int[] errorMagnitudes = findErrorMagnitudes(sigmaOmega[1], errorLocations);
		for (int i = 0; i < errorLocations.length; i++) {
			int position = chunks.length - 1 - GaloisField.log(errorLocations[i]);
			if (position < 0) {
				return false;
			}
			chunks[position] = GaloisField.addOrSubtract(chunks[position], errorMagnitudes[i]);
		}
		return true;
	}
	
	@Nullable
	@Contract(pure = true)
	private static GaloisFieldPoly[] runEuclideanAlgorithm(@NotNull GaloisFieldPoly a, @NotNull GaloisFieldPoly b, int R) {
		if (a.getDegree() < b.getDegree()) {
			GaloisFieldPoly temp = a;
			a = b;
			b = temp;
		}
		
		GaloisFieldPoly rLast = a;
		GaloisFieldPoly r = b;
		GaloisFieldPoly tLast = GaloisField.ZERO;
		GaloisFieldPoly t = GaloisField.ONE;
		
		// Run Euclidean algorithm until r's degree is less than R/2
		while (r.getDegree() >= R / 2) {
			GaloisFieldPoly rLastLast = rLast;
			GaloisFieldPoly tLastLast = tLast;
			rLast = r;
			tLast = t;
			
			// Divide rLastLast by rLast, with quotient in q and remainder in r
			if (rLast.isZero()) {
				throw new IllegalStateException("r_{i-1} was zero, Euclidean algorithm already terminated?");
				//originally ReedSolomonException, but was never able to reproduce
			}
			
			r = rLastLast;
			GaloisFieldPoly q = GaloisField.ZERO;
			int dltInverse = GaloisField.inverse(rLast.getCoefficient(rLast.getDegree()));
			
			while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
				int degreeDiff = r.getDegree() - rLast.getDegree();
				int scale = GaloisField.multiply(r.getCoefficient(r.getDegree()), dltInverse);
				q = q.addOrSubtract(GaloisField.buildMonomial(degreeDiff, scale));
				r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
			}
			
			t = q.multiply(tLast).addOrSubtract(tLastLast);
			if (r.getDegree() >= rLast.getDegree()) {
				throw new IllegalStateException("Failed to reduce polynomial, is correction chunk count less than two?");
			}
		}
		
		int sigmaTildeAtZero = t.getCoefficient(0);
		if (sigmaTildeAtZero == 0) {
			return null;
		}
		
		int inverse = GaloisField.inverse(sigmaTildeAtZero);
		GaloisFieldPoly sigma = t.multiply(inverse);
		GaloisFieldPoly omega = r.multiply(inverse);
		return new GaloisFieldPoly[]{sigma, omega};
	}
	
	@Nullable
	@Contract(pure = true)
	private static int[] findErrorLocations(@NotNull GaloisFieldPoly errorLocator) {
		// This is a direct application of Chien's search
		if (errorLocator.getDegree() == 1) { // shortcut
			return new int[]{errorLocator.getCoefficient(1)};
		}
		
		int[] result = new int[errorLocator.getDegree()];
		int count = 0;
		for (int i = 1; count < result.length && i < GaloisField.SIZE; i++) {
			if (errorLocator.evaluateAt(i) == 0) {
				result[count++] = GaloisField.inverse(i);
			}
		}
		return count == result.length ? result : null;
	}
	
	@NotNull
	@Contract(pure = true)
	private static int[] findErrorMagnitudes(GaloisFieldPoly errorEvaluator, @NotNull int[] errorLocations) {
		// This is directly applying Forney's Formula
		int[] result = new int[errorLocations.length];
		for (int i = 0; i < result.length; i++) {
			int xiInverse = GaloisField.inverse(errorLocations[i]);
			int denominator = 1;
			for (int j = 0; j < result.length; j++) {
				if (i != j) {
					int term = GaloisField.multiply(errorLocations[j], xiInverse);
					denominator = GaloisField.multiply(denominator, (term & 0x1) == 0 ? term | 1 : term & ~1);
				}
			}
			result[i] = GaloisField.multiply(GaloisField.multiply(errorEvaluator.evaluateAt(xiInverse),
					GaloisField.inverse(denominator)), xiInverse);
		}
		return result;
	}
}
