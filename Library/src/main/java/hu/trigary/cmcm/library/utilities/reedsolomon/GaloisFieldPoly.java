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
 * Represents a polynomial whose coefficients are elements of a GF.
 * Instances of this class are immutable.
 */
class GaloisFieldPoly {
	private final int[] coefficients;
	
	/**
	 * @param coefficients coefficients representing elements of GF(size),
	 * arranged from most significant (highest-power term) to least significant
	 */
	GaloisFieldPoly(@NotNull int[] coefficients) {
		if (coefficients.length == 0) {
			throw new IllegalArgumentException("coefficients must be specified");
		} else if (coefficients.length == 1 || coefficients[0] != 0) {
			//noinspection AssignmentOrReturnOfFieldWithMutableType
			this.coefficients = coefficients;
			return;
		}
		
		// Leading term must be non-zero for anything except the constant polynomial "0"
		int firstNonZero = 1;
		while (firstNonZero < coefficients.length && coefficients[firstNonZero] == 0) {
			firstNonZero++;
		}
		if (firstNonZero == coefficients.length) {
			this.coefficients = new int[]{0};
		} else {
			this.coefficients = new int[coefficients.length - firstNonZero];
			System.arraycopy(coefficients, firstNonZero, this.coefficients, 0, this.coefficients.length);
		}
	}
	
	
	
	@NotNull
	@Contract(pure = true)
	int[] getCoefficients() {
		//noinspection AssignmentOrReturnOfFieldWithMutableType
		return coefficients;
	}
	
	/**
	 * @return degree of this polynomial
	 */
	@Contract(pure = true)
	int getDegree() {
		return coefficients.length - 1;
	}
	
	/**
	 * @return true iff this polynomial is the monomial "0"
	 */
	@Contract(pure = true)
	boolean isZero() {
		return coefficients[0] == 0;
	}
	
	/**
	 * @return coefficient of x^degree term in this polynomial
	 */
	@Contract(pure = true)
	int getCoefficient(int degree) {
		return coefficients[coefficients.length - 1 - degree];
	}
	
	/**
	 * @return evaluation of this polynomial at a given point
	 */
	@Contract(pure = true)
	int evaluateAt(int a) {
		if (a == 0) {
			return coefficients[coefficients.length - 1];
		}
		
		if (a == 1) {
			int result = 0;
			for (int coefficient : coefficients) {
				result = GaloisField.addOrSubtract(result, coefficient);
			}
			return result;
		}
		
		int result = coefficients[0];
		int size = coefficients.length;
		for (int i = 1; i < size; i++) {
			result = GaloisField.addOrSubtract(GaloisField.multiply(a, result), coefficients[i]);
		}
		return result;
	}
	
	
	
	@NotNull
	@Contract(pure = true)
	GaloisFieldPoly addOrSubtract(GaloisFieldPoly other) {
		if (isZero()) {
			return other;
		} else if (other.isZero()) {
			return this;
		}
		
		int[] smallerCoefficients = coefficients;
		int[] largerCoefficients = other.coefficients;
		if (smallerCoefficients.length > largerCoefficients.length) {
			int[] temp = smallerCoefficients;
			smallerCoefficients = largerCoefficients;
			largerCoefficients = temp;
		}
		
		int[] sumDiff = new int[largerCoefficients.length];
		int lengthDiff = largerCoefficients.length - smallerCoefficients.length;
		// Copy high-order terms only found in higher-degree polynomial's coefficients
		System.arraycopy(largerCoefficients, 0, sumDiff, 0, lengthDiff);
		
		for (int i = lengthDiff; i < largerCoefficients.length; i++) {
			sumDiff[i] = GaloisField.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
		}
		return new GaloisFieldPoly(sumDiff);
	}
	
	@NotNull
	@Contract(pure = true)
	GaloisFieldPoly multiply(GaloisFieldPoly other) {
		if (isZero() || other.isZero()) {
			return GaloisField.ZERO;
		}
		
		int[] otherCoefficients = other.coefficients;
		int[] product = new int[coefficients.length + otherCoefficients.length - 1];
		for (int i = 0; i < coefficients.length; i++) {
			int coefficient = coefficients[i];
			for (int j = 0; j < otherCoefficients.length; j++) {
				product[i + j] = GaloisField.addOrSubtract(product[i + j], GaloisField.multiply(coefficient, otherCoefficients[j]));
			}
		}
		return new GaloisFieldPoly(product);
	}
	
	@NotNull
	@Contract(pure = true)
	GaloisFieldPoly multiply(int scalar) {
		if (scalar == 0) {
			return GaloisField.ZERO;
		} else if (scalar == 1) {
			return this;
		}
		
		int[] product = new int[coefficients.length];
		//noinspection Convert2streamapi
		for (int i = 0; i < coefficients.length; i++) {
			product[i] = GaloisField.multiply(coefficients[i], scalar);
		}
		return new GaloisFieldPoly(product);
	}
	
	@NotNull
	@Contract(pure = true)
	GaloisFieldPoly multiplyByMonomial(int degree, int coefficient) {
		if (degree < 0) {
			throw new IllegalArgumentException("degree must be non negative");
		} else if (coefficient == 0) {
			return GaloisField.ZERO;
		}
		
		int[] product = new int[coefficients.length + degree];
		for (int i = 0; i < coefficients.length; i++) {
			product[i] = GaloisField.multiply(coefficients[i], coefficient);
		}
		return new GaloisFieldPoly(product);
	}
	
	@NotNull
	@Contract(pure = true)
	GaloisFieldPoly[] divide(@NotNull GaloisFieldPoly other) {
		if (other.isZero()) {
			throw new IllegalArgumentException("must not divide by zero");
		}
		
		int inverseDenominatorLeadingTerm = GaloisField.inverse(other.getCoefficient(other.getDegree()));
		GaloisFieldPoly quotient = GaloisField.ZERO;
		GaloisFieldPoly remainder = this;
		
		while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
			int degreeDifference = remainder.getDegree() - other.getDegree();
			int scale = GaloisField.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
			GaloisFieldPoly term = other.multiplyByMonomial(degreeDifference, scale);
			GaloisFieldPoly iterationQuotient = GaloisField.buildMonomial(degreeDifference, scale);
			quotient = quotient.addOrSubtract(iterationQuotient);
			remainder = remainder.addOrSubtract(term);
		}
		return new GaloisFieldPoly[]{quotient, remainder};
	}
}
