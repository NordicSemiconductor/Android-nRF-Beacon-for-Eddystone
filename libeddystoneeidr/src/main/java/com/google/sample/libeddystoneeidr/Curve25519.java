// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sample.libeddystoneeidr;

import java.util.Arrays;

/**
 * Class providing the primitives of Curve25519; in particular, the ability of retrieving the x
 * coordinate of nQ given n and the x coordinate of a point Q on the curve, and the special case
 * where Q is the base point of Curve25519.
 */
class Curve25519 {
  private static final byte[] BASE_POINT = new byte[]{
      9, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0
  };

  /**
   * Modifies {@code source} to a Curve25519 private key.
   */
  /* @VisibleForTesting */ static void toPrivateKey(byte[] source) {
    if (source.length != 32) {
      throw new IllegalArgumentException("All keys must be exactly 32 bytes long.");
    }
    source[0] &= 248;
    source[31] &= 127;
    source[31] |= 64;
  }

  /**
   * Returns the x coordinates of the point nQ, where Q is the base point.
   *
   * <p>Used to generate a public key from a secret key.
   */
  public static byte[] scalarMultBase(byte[] n) {
    toPrivateKey(n);
    return scalarMult(n, BASE_POINT);
  }

  /**
   * Returns the x coordinates of the point nQ, where Q is a point with x coordinate equal to q.
   *
   * <p>Used to generate a shared secret between the owner of the (secret) n and the owner of the
   * secret paired with q.
   */
  public static byte[] scalarMult(byte[] n, byte[] q) {
    if (n.length != 32 || q.length != 32) {
      throw new IllegalArgumentException("All keys must be exactly 32 bytes long.");
    }
    toPrivateKey(n);
    Polynomial25519 qPolynomial = new Polynomial25519(q);
    Polynomial25519.Montgomery m = Polynomial25519.multiple(n, qPolynomial);
    Polynomial25519 reciprocal = m.z.reciprocal();
    reciprocal.mult(m.x);
    return reciprocal.toBytes();
  }

  /**
   * An element of F_{2^255 - 19} in its polynomial form: given an object defined by an array c, the
   * corresponding polynomial is sum(2^(ceil(25.5 * i) * c[i] * x^i), and the element is the
   * valuation of the polynomial at 1.
   *
   * <p>The "reduced degree" form has degree less than 10; the extended form has degree less than
   * 19, and is used to handle the output of the multiplication of these elements. Some operations
   * assume to act on reduced degree polynomials.
   *
   * <p>The "reduced coefficients" form has coefficients less than 2^26 in absolute value. The
   * extended coefficient form has coefficient less than 2^62 in absolute value. Some operations
   * assume to act on reduced coefficients polynomials.
   *
   * <p>It is a responsibility of the caller to make sure to use the correct form whenever
   * necessary.
   *
   * <p>See the paper [1] Curve25519: new Diffie-Hellman speed records at
   * http://cr.yp.to/ecdh/curve25519-20060209.pdf for more information
   */
  /* @VisibleForTesting */ static class Polynomial25519 {
    private long[] c = new long[19];

    /**
     * An element of F_{2^255-19} in the "Montgomery" form, i.e. the element corresponding to (x, z)
     * is x/z = x * z^{-1}.
     */
    private static class Montgomery {
      public Polynomial25519 x;
      public Polynomial25519 z;

      public Montgomery(Polynomial25519 x, Polynomial25519 z) {
        this.x = new Polynomial25519(x);
        this.z = new Polynomial25519(z);
      }

      public Montgomery(Montgomery m) {
        this(m.x, m.z);
      }
    }

    public Polynomial25519() {}

    /**
     * After, "this" is of reduced degree and of reduced coefficients.
     * @param x An integer less than 2^26 in absolute value.
     */
    public Polynomial25519(long x) {
      c[0] = x;
    }

    /**
     * Copy constructor.
     * After, "this" is of reduced degree.
     * @param other A reduced degree polynomial.
     */
    public Polynomial25519(Polynomial25519 other) {
      for (int i = 0; i < 10; i++) {
        c[i] = other.c[i];
      }
    }

    /**
     * Only used for testing.
     */
    public Polynomial25519(long[] other) {
      for (int i = 0; i < other.length && i < 19; i++) {
        c[i] = other[i];
      }
    }

    /**
     * Constructs a polynomial from a 32-bytes representation.
     */
    public Polynomial25519(byte[] bytes) {
      if (bytes.length != 32) {
        throw new IllegalArgumentException("bytes must have length 32");
      }
      c[0] = coefficientFromBytes(bytes, 0, 0, 0x3ffffff);
      c[1] = coefficientFromBytes(bytes, 3, 2, 0x1ffffff);
      c[2] = coefficientFromBytes(bytes, 6, 3, 0x3ffffff);
      c[3] = coefficientFromBytes(bytes, 9, 5, 0x1ffffff);
      c[4] = coefficientFromBytes(bytes, 12, 6, 0x3ffffff);
      c[5] = coefficientFromBytes(bytes, 16, 0, 0x1ffffff);
      c[6] = coefficientFromBytes(bytes, 19, 1, 0x3ffffff);
      c[7] = coefficientFromBytes(bytes, 22, 3, 0x1ffffff);
      c[8] = coefficientFromBytes(bytes, 25, 4, 0x3ffffff);
      c[9] = coefficientFromBytes(bytes, 28, 6, 0x1ffffff);
    }

    /**
     * Returns a single byte of the 32-bytes representation.
     */
    private static long coefficientFromBytes(byte[] input, int start, int shift, int mask) {
      return ((((input[start + 0] & 0xff) << 0)
          | ((input[start + 1] & 0xff) << 8)
          | ((input[start + 2] & 0xff) << 16)
          | ((input[start + 3] & 0xff) << 24)) >> shift) & mask;
    }

    /**
     * Returns a 32-bytes representation of the element
     */
    public byte[] toBytes() {
      byte[] output = new byte[32];
      do {
        for (int i = 0; i < 9; i++) {
          if ((i & 1) == 1) {
            while (c[i] < 0) {
              c[i] += 0x2000000;
              c[i + 1]--;
            }
          } else {
            while (c[i] < 0) {
              c[i] += 0x4000000;
              c[i + 1]--;
            }
          }
        }
        while (c[9] < 0) {
          c[9] += 0x2000000;
          c[0] -= 19;
        }
      } while (c[0] < 0);
      c[1] <<= 2;
      c[2] <<= 3;
      c[3] <<= 5;
      c[4] <<= 6;
      c[6] <<= 1;
      c[7] <<= 3;
      c[8] <<= 4;
      c[9] <<= 6;
      output[0] = 0;
      output[16] = 0;
      bytesFromCoefficients(output, 0, 0);
      bytesFromCoefficients(output, 1, 3);
      bytesFromCoefficients(output, 2, 6);
      bytesFromCoefficients(output, 3, 9);
      bytesFromCoefficients(output, 4, 12);
      bytesFromCoefficients(output, 5, 16);
      bytesFromCoefficients(output, 6, 19);
      bytesFromCoefficients(output, 7, 22);
      bytesFromCoefficients(output, 8, 25);
      bytesFromCoefficients(output, 9, 28);
      return output;
    }

    private void bytesFromCoefficients(byte[] output, int idx, int start) {
      output[start + 0] |= c[idx] & 0xff;
      output[start + 1] = (byte) ((c[idx] >> 8) & 0xff);
      output[start + 2] = (byte) ((c[idx] >> 16) & 0xff);
      output[start + 3] = (byte) ((c[idx] >> 24) & 0xff);
    }

    /**
     * Clears the high-degree part of the polynomial, trusting the caller that that part is not
     * used.
     */
    public void clean() {
      Arrays.fill(c, 10, 19, 0);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Polynomial25519 other = (Polynomial25519) obj;
      for (int i = 0; i < 19; i++) {
        if (other.c[i] != c[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(c);
    }

    public String toString() {
      String s = "";
      for (int i = 0; i < 10; i++) {
        s += c[i] + " ";
      }
      s += "    ";
      for (int i = 10; i < 19; i++) {
        s += c[i] + " ";
      }
      return s;
    }

    /**
     * Computes the x coordinate of a point nQ, where Q is one of the preimages of q = q/1 in the
     * elliptic curve. It is proven that any preimage Q gives the same result.
     * @param n A number in the 32-bytes little endian format.
     * @param q The x-coordinates of a point in the curve.
     * @return The x coordinate of nQ, in the Montgomery form (first/second). Both polynomials are
     *     in the reduced degree form.
     */
    public static Montgomery multiple(byte[] n, Polynomial25519 q) {
      Montgomery nqpq[] = new Montgomery[] {
          new Montgomery(new Polynomial25519(q), new Polynomial25519(1)),
          new Montgomery(new Polynomial25519(0), new Polynomial25519(1))
      };
      Montgomery nq[] = new Montgomery[] {
          new Montgomery(new Polynomial25519(1), new Polynomial25519(0)),
          new Montgomery(new Polynomial25519(0), new Polynomial25519(1))
      };

      int rollIdx = 0;
      for (int i = 0; i < 32; i++) {
        byte b = n[31 - i];
        for (int j = 0; j < 8; j++) {
          int bit = -(b >> 7);

          swapConditional(nq[rollIdx % 2].x, nqpq[rollIdx % 2].x, bit);
          swapConditional(nq[rollIdx % 2].z, nqpq[rollIdx % 2].z, bit);

          montgomery(nq[rollIdx % 2], nqpq[rollIdx % 2], q,
              nq[(rollIdx + 1) % 2], nqpq[(rollIdx + 1) % 2]);

          swapConditional(nq[(rollIdx + 1) % 2].x, nqpq[(rollIdx + 1) % 2].x, bit);
          swapConditional(nq[(rollIdx + 1) % 2].z, nqpq[(rollIdx + 1) % 2].z, bit);

          rollIdx++;
          b <<= 1;
        }
      }
      return nq[rollIdx % 2];
    }

    /**
     * Adds other to the object.
     * Before, "this" must be of reduced degree.
     * After, "this" is of reduced degree.
     * @param other A reduced degree polynomial.
     */
    public void sum(Polynomial25519 other) {
      for (int i = 0; i < 10; i++) {
        c[i] += other.c[i];
      }
    }

    /**
     * Subtracts other from the object.
     * Before, "this" must be of reduced degree.
     * After, "this" is of reduced degree.
     * @param other A reduced degree polynomial.
     */
    public void diff(Polynomial25519 other) {
      for (int i = 0; i < 10; i++) {
        c[i] -= other.c[i];
      }
    }

    /**
     * Multiply the object by a scalar.
     * Before, "this" must be of reduced degree and of reduced coefficients.
     * After, "this" is of reduced degree and of reduced coefficients.
     * @param scalar An integer less than 2^26.
     */
    public void mult(int scalar) {
      for (int i = 0; i < 10; i++) {
        c[i] *= scalar;
      }
      reduceCoefficients();
    }

    /**
     * Non-static version of {@code mult}.
     */
    public void mult(Polynomial25519 other) {
      c = Polynomial25519.innerMult(c, other.c);
      reduceDegree();
      reduceCoefficients();
    }

    /**
     * Returns a polynomial corresponding to the multiplication of the two polynomials in input.
     * @param a A reduced degree, reduced coefficients polynomial.
     * @param b A reduced degree, reduced coefficients polynomial.
     * @return A reduced degree, reduced coefficients polynomial.
     */
    public static Polynomial25519 mult(Polynomial25519 a, Polynomial25519 b) {
      Polynomial25519 output = new Polynomial25519();
      output.c = Polynomial25519.innerMult(a.c, b.c);
      output.reduceDegree();
      output.reduceCoefficients();
      return output;
    }

    /**
     * Returns a polynomial corresponding to the square of the input polynomial.
     * @param a A reduced degree, reduced coefficients polynomial.
     * @return A reduced degree, reduced coefficients polynomial.
     */
    public static Polynomial25519 square(Polynomial25519 a) {
      Polynomial25519 output = new Polynomial25519();
      output.c = Polynomial25519.innerSquare(a.c);
      output.reduceDegree();
      output.reduceCoefficients();
      return output;
    }

    /**
     * Computes the square of this polynomial.
     * Before, "this" must be of reduced degree, reduced coefficients.
     * After, "this" is of reduced degree, reduced coefficients.
     */
    public void square() {
      c = Polynomial25519.innerSquare(c);
      reduceDegree();
      reduceCoefficients();
    }

    /**
     * Computes the reciprocal of the input by elevating to the (2^255 - 19) - 2 efficiently.
     * "This" must be a reduced degree, reduced coefficient polynomial.
     */
    public Polynomial25519 reciprocal() {
      return innerReciprocal(this);
    }

    /**
     * Reduce the degree of the polynomial.
     * Before, "this" must be of extended degree.
     * After, "this" is of reduced degree.
     */
    private void reduceDegree() {
      for (int i = 8; i >= 0; i--) {
        // Means adding 19 times the other.
        c[i] += c[10 + i] << 4;
        c[i] += c[10 + i] << 1;
        c[i] += c[10 + i];
      }
      // We do not need to zero the upper part, as following operations will assume they are not
      // used.
    }

    /**
     * Reduce the coefficients of the polynomial.
     * Before, "this" must be of reduced degree.
     * After, "this" is of reduced degree, reduced coefficients.
     */
    private void reduceCoefficients() {
      do {
        c[10] = 0;
        for (int i = 0; i < 10; i += 2) {
          long over = c[i] / 0x4000000;
          c[i + 1] += over;
          c[i] -= over * 0x4000000;

          over = c[i + 1] / 0x2000000;
          c[i + 2] += over;
          c[i + 1] -= over * 0x2000000;
        }
        c[0] += 19 * c[10];
      } while (c[10] != 0);
    }

    /**
     * Swaps a and b if iswap is 1, do nothing (but using the same time) if iswap is 0. Any other
     * value is not accepted.
     * @param a A reduced degree polynomial.
     * @param b A reduced degree polynomial.
     * @param iswap Whether should swap or not.
     */
    private static void swapConditional(Polynomial25519 a, Polynomial25519 b, long iswap) {
      int swap = (int) (-iswap);
      for (int i = 0; i < 10; i++) {
        int x = swap & (((int) a.c[i]) ^ ((int) b.c[i]));
        a.c[i] = ((int) a.c[i]) ^ x;
        b.c[i] = ((int) b.c[i]) ^ x;
      }
    }

    /**
     * Computes the x coordinates of 2Q and Q+P. See appendix B (page 21) in [1].
     * @param q The x coordinate of a point Q on the curve, reduced degree form. Destroyed.
     * @param p The x coordinate of a point P on the curve, reduced degree form. Destroyed.
     * @param qmp The x coordinate of the point Q-P on the curve, reduced degree form. Preserved.
     * @param qpq Output 2Q, in reduced degree, reduced coefficients form.
     * @param qpp Output Q+P, in reduced degree, reduced coefficients form.
     */
    private static void montgomery(Montgomery q, Montgomery p, Polynomial25519 qmp,
                                   Montgomery qpq, Montgomery qpp) {
      Montgomery qprime = new Montgomery(q);
      qprime.x.sum(q.z);                               // q'.x = q.x + q.z
      qprime.z.diff(q.x);                              // q'.z = q.z - q.x

      Montgomery pprime = new Montgomery(p);
      pprime.x.sum(p.z);                               // p'.x = p.x + p.z
      pprime.z.diff(p.x);                              // p'.z = p.z - p.x

      pprime.x.mult(qprime.z);                         // p'.x *= q'.z
      pprime.z.mult(qprime.x);                         // p'.z *= q'.x

      qprime.x.square();                               // q'.x **= 2
      qprime.z.square();                               // q'.z **= 2

      qpp.x = new Polynomial25519(pprime.x);
      qpp.z = new Polynomial25519(pprime.z);
      qpp.x.sum(pprime.z);                             // (q+p).x = p'.x + p'.z
      qpp.z.diff(pprime.x);                            // (q+p).z = p'.z - p'.x

      qpp.x.square();                                  // (q+p).x **= 2
      qpp.z.square();                                  // (q+p).z **= 2
      qpp.z.mult(qmp);                                 // (q+p).z *= (q-p)

      qpq.x = new Polynomial25519(qprime.x);
      qpq.z = new Polynomial25519(qprime.x);
      qpq.x.mult(qprime.z);                            // (2q).x = q'.x * q'.z
      qpq.z.diff(qprime.z);                            // (2q).z = q'.x - q'.z

      Polynomial25519 t = new Polynomial25519(qpq.z);
      qpq.z.mult(121665);                              // (2q).z *= (A - 2) / 4
      qpq.z.sum(qprime.x);                             // (2q).z += q'.x
      qpq.z.mult(t);                                   // (2q).z += t
    }


    /**
     * Returns a polynomial corresponding to the multiplication of the two polynomials in input.
     * @param a A reduced degree, reduced coefficients polynomial.
     * @param b A reduced degree, reduced coefficients polynomial.
     * @return The product polynomial.
     */
    private static long[] innerMult(long[] a, long[] b) {
      long[] output = new long[19];
      output[0] =
          b[0] * a[0];
      output[1] =
          b[0] * a[1]
              + b[1] * a[0];
      output[2] =
          b[1] * a[1] * 2
              + b[0] * a[2]
              + b[2] * a[0];
      output[3] =
          b[1] * a[2]
              + b[2] * a[1]
              + b[0] * a[3]
              + b[3] * a[0];
      output[4] =
          b[2] * a[2]
              + (b[1] * a[3]
              + b[3] * a[1]) * 2
              + b[0] * a[4]
              + b[4] * a[0];
      output[5] =
          b[2] * a[3]
              + b[3] * a[2]
              + b[1] * a[4]
              + b[4] * a[1]
              + b[0] * a[5]
              + b[5] * a[0];
      output[6] =
          (b[3] * a[3]
              + b[1] * a[5]
              + b[5] * a[1]) * 2
              + b[2] * a[4]
              + b[4] * a[2]
              + b[0] * a[6]
              + b[6] * a[0];
      output[7] =
          b[3] * a[4]
              + b[4] * a[3]
              + b[2] * a[5]
              + b[5] * a[2]
              + b[1] * a[6]
              + b[6] * a[1]
              + b[0] * a[7]
              + b[7] * a[0];
      output[8] =
          b[4] * a[4]
              + (b[3] * a[5]
              + b[5] * a[3]
              + b[1] * a[7]
              + b[7] * a[1]) * 2
              + b[2] * a[6]
              + b[6] * a[2]
              + b[0] * a[8]
              + b[8] * a[0];
      output[9] =
          b[4] * a[5]
              + b[5] * a[4]
              + b[3] * a[6]
              + b[6] * a[3]
              + b[2] * a[7]
              + b[7] * a[2]
              + b[1] * a[8]
              + b[8] * a[1]
              + b[0] * a[9]
              + b[9] * a[0];
      output[10] =
          (b[5] * a[5]
              + b[3] * a[7]
              + b[7] * a[3]
              + b[1] * a[9]
              + b[9] * a[1]) * 2
              + b[4] * a[6]
              + b[6] * a[4]
              + b[2] * a[8]
              + b[8] * a[2];
      output[11] =
          b[5] * a[6]
              + b[6] * a[5]
              + b[4] * a[7]
              + b[7] * a[4]
              + b[3] * a[8]
              + b[8] * a[3]
              + b[2] * a[9]
              + b[9] * a[2];
      output[12] =
          b[6] * a[6]
              + (b[5] * a[7]
              + b[7] * a[5]
              + b[3] * a[9]
              + b[9] * a[3]) * 2
              + b[4] * a[8]
              + b[8] * a[4];
      output[13] =
          b[6] * a[7]
              + b[7] * a[6]
              + b[5] * a[8]
              + b[8] * a[5]
              + b[4] * a[9]
              + b[9] * a[4];
      output[14] =
          (b[7] * a[7]
              + b[5] * a[9]
              + b[9] * a[5]) * 2
              + b[6] * a[8]
              + b[8] * a[6];
      output[15] =
          b[7] * a[8]
              + b[8] * a[7]
              + b[6] * a[9]
              + b[9] * a[6];
      output[16] =
          b[8] * a[8]
              + (b[7] * a[9]
              + b[9] * a[7]) * 2;
      output[17] =
          b[8] * a[9]
              + b[9] * a[8];
      output[18] =
          b[9] * a[9] * 2;
      return output;
    }

    /**
     * Returns a polynomial corresponding to the square of the input polynomial.
     * @param a A reduced degree, reduced coefficients polynomial.
     * @return The squared polynomial.
     */
    private static long[] innerSquare(long[] a) {
      long[] output = new long[19];
      output[0] =
          a[0] * a[0];
      output[1] =
          a[0] * a[1] * 2;
      output[2] =
          (a[1] * a[1]
              + a[0] * a[2]) * 2;
      output[3] =
          (a[1] * a[2]
              + a[0] * a[3]) * 2;
      output[4] =
          a[2] * a[2]
              + a[1] * a[3] * 4
              + a[0] * a[4] * 2;
      output[5] =
          (a[2] * a[3]
              + a[1] * a[4]
              + a[0] * a[5]) * 2;
      output[6] =
          (a[3] * a[3]
              + a[2] * a[4]
              + a[0] * a[6]
              + a[1] * a[5] * 2) * 2;
      output[7] =
          (a[3] * a[4]
              + a[2] * a[5]
              + a[1] * a[6]
              + a[0] * a[7]) * 2;
      output[8] =
          a[4] * a[4]
              + (a[2] * a[6]
              + a[0] * a[8]
              + (a[1] * a[7]
              + a[3] * a[5]) * 2) * 2;
      output[9] =
          (a[4] * a[5]
              + a[3] * a[6]
              + a[2] * a[7]
              + a[1] * a[8]
              + a[0] * a[9]) * 2;
      output[10] = (a[5] * a[5]
          + a[4] * a[6]
          + a[2] * a[8]
          + (a[3] * a[7]
          + a[1] * a[9]) * 2) * 2;
      output[11] =
          (a[5] * a[6]
              + a[4] * a[7]
              + a[3] * a[8]
              + a[2] * a[9]) * 2;
      output[12] =
          a[6] * a[6]
              + (a[4] * a[8]
              + (a[5] * a[7]
              + a[3] * a[9]) * 2) * 2;
      output[13] =
          (a[6] * a[7]
              + a[5] * a[8]
              + a[4] * a[9]) * 2;
      output[14] =
          (a[7] * a[7]
              + a[6] * a[8]
              + a[5] * a[9] * 2) * 2;
      output[15] =
          (a[7] * a[8]
              + a[6] * a[9]) * 2;
      output[16] =
          a[8] * a[8]
              + a[7] * a[9] * 4;
      output[17] =
          a[8] * a[9] * 2;
      output[18] =
          a[9] * a[9] * 2;
      return output;
    }

    /**
     * Computes the reciprocal of the input by elevating to the (2^255 - 19) - 2 efficiently.
     * "This" must be a reduced degree, reduced coefficient polynomial.
     */
    private static Polynomial25519 innerReciprocal(Polynomial25519 z) {
      // In the comment we wrote the exponent of the input.
      // TODO: can avoid t1?
      /* 2 */ Polynomial25519 z2 = Polynomial25519.square(z);
      /* 4 */ Polynomial25519 t1 = Polynomial25519.square(z2);
      /* 8 */ Polynomial25519 t0 = Polynomial25519.square(t1);
      /* 9 */ Polynomial25519 z9 = Polynomial25519.mult(t0, z);
      /* 11 */ Polynomial25519 z11 = Polynomial25519.mult(z9, z2);
      /* 22 */ t0 = Polynomial25519.square(z11);
      /* 31 = 2^5 - 2^0 */ Polynomial25519 z2FiveZero = Polynomial25519.mult(t0, z9);

      /* 2^6 - 2^1 */ t0 = Polynomial25519.square(z2FiveZero);
      /* 2^7 - 2^2 */ t1 = Polynomial25519.square(t0);
      /* 2^8 - 2^3 */ t0 = Polynomial25519.square(t1);
      /* 2^9 - 2^4 */ t1 = Polynomial25519.square(t0);
      /* 2^10 - 2^5 */ t0 = Polynomial25519.square(t1);
      /* 2^10 - 2^0 */ Polynomial25519 z2TenZero = Polynomial25519.mult(t0, z2FiveZero);

      /* 2^11 - 2^1 */ t0 = Polynomial25519.square(z2TenZero);
      /* 2^12 - 2^2 */ t1 = Polynomial25519.square(t0);
      /* 2^20 - 2^10 */
      for (int i = 2; i < 10; i += 2) {
        t0 = Polynomial25519.square(t1);
        t1 = Polynomial25519.square(t0);
      }
      /* 2^20 - 2^0 */ Polynomial25519 z2TwentyZero = Polynomial25519.mult(t1, z2TenZero);

      /* 2^21 - 2^1 */ t0 = Polynomial25519.square(z2TwentyZero);
      /* 2^22 - 2^2 */ t1 = Polynomial25519.square(t0);
      /* 2^40 - 2^20 */
      for (int i = 2; i < 20; i += 2) {
        t0 = Polynomial25519.square(t1);
        t1 = Polynomial25519.square(t0);
      }
      /* 2^40 - 2^0 */ t0 = Polynomial25519.mult(t1, z2TwentyZero);

      /* 2^41 - 2^1 */ t1 = Polynomial25519.square(t0);
      /* 2^42 - 2^2 */ t0 = Polynomial25519.square(t1);
      /* 2^50 - 2^10 */
      for (int i = 2; i < 10; i += 2) {
        t1 = Polynomial25519.square(t0);
        t0 = Polynomial25519.square(t1);
      }
      /* 2^50 - 2^0 */ Polynomial25519 z2FiftyZero = Polynomial25519.mult(t0, z2TenZero);

      /* 2^51 - 2^1 */ t0 = Polynomial25519.square(z2FiftyZero);
      /* 2^52 - 2^2 */ t1 = Polynomial25519.square(t0);
      /* 2^100 - 2^50 */
      for (int i = 2; i < 50; i += 2) {
        t0 = Polynomial25519.square(t1);
        t1 = Polynomial25519.square(t0);
      }
      /* 2^100 - 2^0 */ Polynomial25519 z2HundredZero = Polynomial25519.mult(t1, z2FiftyZero);

      /* 2^101 - 2^1 */ t1 = Polynomial25519.square(z2HundredZero);
      /* 2^102 - 2^2 */ t0 = Polynomial25519.square(t1);
      /* 2^200 - 2^100 */
      for (int i = 2; i < 100; i += 2) {
        t1 = Polynomial25519.square(t0);
        t0 = Polynomial25519.square(t1);
      }
      /* 2^200 - 2^0 */ t1 = Polynomial25519.mult(t0, z2HundredZero);

      /* 2^201 - 2^1 */ t0 = Polynomial25519.square(t1);
      /* 2^202 - 2^2 */ t1 = Polynomial25519.square(t0);
      /* 2^250 - 2^50 */
      for (int i = 2; i < 50; i += 2) {
        t0 = Polynomial25519.square(t1);
        t1 = Polynomial25519.square(t0);
      }
      /* 2^250 - 2^0 */ t0 = Polynomial25519.mult(t1, z2FiftyZero);

      /* 2^251 - 2^1 */ t1 = Polynomial25519.square(t0);
      /* 2^252 - 2^2 */ t0 = Polynomial25519.square(t1);
      /* 2^253 - 2^3 */ t1 = Polynomial25519.square(t0);
      /* 2^254 - 2^4 */ t0 = Polynomial25519.square(t1);
      /* 2^255 - 2^5 */ t1 = Polynomial25519.square(t0);
      /* 2^255 - 21 */ return Polynomial25519.mult(t1, z11);
    }

  }
}
