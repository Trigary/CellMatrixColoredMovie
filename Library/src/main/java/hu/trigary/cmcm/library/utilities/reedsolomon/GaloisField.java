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

/**
 * This class contains utility methods for performing mathematical operations over
 * the Galois Fields. Operations use a given primitive polynomial in calculations.
 *
 * This field is the same as AZTEC_DATA_12's field: x^12 + x^6 + x^5 + x^3 + 1
 * <br>
 * 4096 = 2^12 = 2^(4*3) -- each chunk consist of 4 pixels, all of which contain 3 bits
 */
final class GaloisField {
	public static final int SIZE = 4096;
	static final GaloisFieldPoly ZERO = new GaloisFieldPoly(new int[]{0});
	static final GaloisFieldPoly ONE = new GaloisFieldPoly(new int[]{1});
	private static final int PRIMITIVE = 0x1069;
	private static final int[] EXP_TABLE = new int[SIZE];
	private static final int[] LOG_TABLE = new int[SIZE];
	
	static {
		int x = 1;
		for (int i = 0; i < SIZE; i++) {
			EXP_TABLE[i] = x;
			x *= 2; // we're assuming the generator alpha is 2
			if (x >= SIZE) {
				x ^= PRIMITIVE;
				x &= SIZE - 1;
			}
		}
		
		for (int i = 0; i < SIZE - 1; i++) {
			LOG_TABLE[EXP_TABLE[i]] = i;
		}
	}
	
	private GaloisField() { }
	
	/**
	 * @return the monomial representing coefficient * x^degree
	 */
	@NotNull
	@Contract(pure = true)
	static GaloisFieldPoly buildMonomial(int degree, int coefficient) {
		if (degree < 0) {
			throw new IllegalArgumentException("degree must be non negative");
		} else if (coefficient == 0) {
			return ZERO;
		}
		
		int[] coefficients = new int[degree + 1];
		coefficients[0] = coefficient;
		return new GaloisFieldPoly(coefficients);
	}
	
	/**
	 * Implements both addition and subtraction - they are the same in GF(size).
	 *
	 * @return sum/difference of a and b
	 */
	@Contract(pure = true)
	static int addOrSubtract(int a, int b) {
		return a ^ b;
	}
	
	/**
	 * @return 2 to the power of a in GF(size)
	 */
	@Contract(pure = true)
	static int exp(int a) {
		return EXP_TABLE[a];
	}
	
	/**
	 * @return base 2 log of a in GF(size)
	 */
	@Contract(pure = true)
	static int log(int a) {
		if (a == 0) {
			throw new IllegalArgumentException("parameter must never be 0");
		}
		return LOG_TABLE[a];
	}
	
	/**
	 * @return multiplicative inverse of a
	 */
	@Contract(pure = true)
	static int inverse(int a) {
		if (a == 0) {
			throw new IllegalArgumentException("parameter must never be 0");
		}
		return EXP_TABLE[SIZE - LOG_TABLE[a] - 1];
	}
	
	/**
	 * @return product of a and b in GF(size)
	 */
	@Contract(pure = true)
	static int multiply(int a, int b) {
		return a == 0 || b == 0 ? 0 : EXP_TABLE[(LOG_TABLE[a] + LOG_TABLE[b]) % (SIZE - 1)];
	}
}
