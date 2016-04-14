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

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


/**
 * A sample implementation of Eddystone EIDR computation.
 */
public class EddystoneEidrGenerator {
  private static final String TAG = EddystoneEidrGenerator.class.getSimpleName();

  public static final int MIN_ROTATION_PERIOD_EXPONENT = 0;
  public static final int MAX_ROTATION_PERIOD_EXPONENT = 15;

  // The server's public ECDH Curve25519 key. Must be 32 bytes.
  private byte[] serviceEcdhPublicKey;

  // The beacon's private key. Randomly generated 32-byte data.
  private byte[] beaconPrivateKey;

  // The beacon's public key computed from the private key over ECDH Curve25519.
  private byte[] beaconPublicKey;

  // In some test scenarios we may want to broadcast EIDs from a known Identity Key.
  private byte[] beaconIdentityKey;

  /**
   * Constructs an EddystoneEidrGenerator instance with a real beacon private key and a real
   * service public key.
   *
   * @param serviceEcdhPublicKey 32-byte public key of remote server
   * @param beaconEcdhPrivateKey 32-byte private key of beacon
   */
  public EddystoneEidrGenerator(byte[] serviceEcdhPublicKey, byte[] beaconEcdhPrivateKey) {
    checkArgument(serviceEcdhPublicKey != null && serviceEcdhPublicKey.length == 32);
    checkArgument(beaconEcdhPrivateKey != null && beaconEcdhPrivateKey.length == 32);
    this.serviceEcdhPublicKey = serviceEcdhPublicKey;
    this.beaconPrivateKey = beaconEcdhPrivateKey;
    beaconPublicKey = generateBeaconPublicKey();
  }

  /**
   * Getter for the beacon Roshan
   */
  public byte [] getBeaconPublicKey(){
    if(beaconPublicKey != null)
      return  beaconPublicKey;
    return null;
  }

  /**
   * Constructs an EddystoneEidrGenerator instance with a fake beacon private key and a real
   * service public key.
   *
   * @param serviceEcdhPublicKey 32-byte public key of remote server
   */
  public EddystoneEidrGenerator(byte[] serviceEcdhPublicKey) {
    checkArgument(serviceEcdhPublicKey != null && serviceEcdhPublicKey.length == 32);
    this.serviceEcdhPublicKey = serviceEcdhPublicKey;
    beaconPrivateKey = generateBeaconPrivateKey();
    beaconPublicKey = generateBeaconPublicKey();
  }

  /**
   * Constructs an "empty" EddystoneEidrGenerator instance for use with a known Identity Key
   * set via {@link #setIdentityKey(byte[])}.
   */
  EddystoneEidrGenerator() { }



  public static String bytesToHex(final byte[] bytes, final boolean add0x) {
    if (bytes == null)
      return "";
    return bytesToHex(bytes, 0, bytes.length, add0x);
  }
  private static final char[] HEX_ARRAY = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  public static String bytesToHex(final byte[] bytes, final int start, final int length, final boolean add0x) {
    if (bytes == null || bytes.length <= start || length <= 0)
      return "";

    final int maxLength = Math.min(length, bytes.length - start);
    final char[] hexChars = new char[maxLength * 2];
    for (int j = 0; j < maxLength; j++) {
      final int v = bytes[start + j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    if (!add0x)
      return new String(hexChars);
    return "0x" + new String(hexChars);
  }

  /**
   * Sets the beacon's identity key. When set, this generator will skip the earlier steps of the
   * crypto process that create an identity key (via ECDH, etc).
   *
   * Such use would look like:
   * <code>
   *     byte[] identityKey = getMyBeaconsIdentityKeyFromSomewhere();
   *     int rotationExponent = getMyBeaconsRotationExponentFromThatSamePlace();
   *     long beaconClockOffset = getMyBeaconsClockOffsetFromThatSamePlace();
   *     long beaconNowMs = System.currentTimeMillis() - beaconClockOffset;
   *     EddystoneEidrGenerator eidGen = new EddystoneEidrGenerator();
   *     eidGen.setIdentityKey(identityKey);
   *     byte[] ephemeralId = eidGen(rotationExponent, (int)(beaconNowMs / 1000));
   * </code>
   *
   * @param beaconIdentityKey
   */
  public void setIdentityKey(byte[] beaconIdentityKey) {
    this.beaconIdentityKey = beaconIdentityKey;
    // When using a given identity key, the beacon's public and private keys are unused. Nullify
    // them to make sure.
    beaconPublicKey = null;
    beaconPrivateKey = null;
  }

  /**
   * Returns the 8-byte ephemeral identifier given the rotation period exponent and a timestamp.
   *
   * @param rotationPeriodExponent  A value between {@code MIN_ROTATION_PERIOD_EXPONENT} and
   *                                {@code MAX_ROTATION_PERIOD_EXPONENT} indicating that the
   *                                beacon will rotate its EID every 2^rot seconds
   * @param timestampSeconds  The current time in seconds
   * @return the 8-byte ephemeral identifier or null if the identifier could not be computed
   */
  public byte[] getEidr(int rotationPeriodExponent, int timestampSeconds) {
    checkArgument(rotationPeriodExponent >= MIN_ROTATION_PERIOD_EXPONENT
        && rotationPeriodExponent <= MAX_ROTATION_PERIOD_EXPONENT);
    return getEidrInternal(rotationPeriodExponent, timestampSeconds);
  }

  /**
   * Returns the beacon's public key.
   */
  public byte[] generateBeaconPublicKey() {
    return Curve25519.scalarMultBase(beaconPrivateKey);
  }


  byte[] generateBeaconPrivateKey() {
    UUID uuid = UUID.randomUUID();
    return ByteBuffer.allocate(32)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits()).array();
  }

  byte[] getSharedSecret() {
    return Curve25519.scalarMult(beaconPrivateKey, serviceEcdhPublicKey);
  }

  public byte[]getIdentityKey() {
    if (beaconIdentityKey != null) {
      return beaconIdentityKey;
    }
    Mac hkdfSha256Mac;
    try {
      hkdfSha256Mac = Mac.getInstance("HmacSHA256");
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG, "Error constructing SHA256 HMAC instance", e);
      return null;  // XXX fix callers to cope with null
    }

    byte[] publicKeys = new byte[serviceEcdhPublicKey.length + beaconPublicKey.length];
    System.arraycopy(serviceEcdhPublicKey, 0, publicKeys, 0, serviceEcdhPublicKey.length);
    System.arraycopy(beaconPublicKey, 0, publicKeys, serviceEcdhPublicKey.length, beaconPublicKey.length);

    try {
      hkdfSha256Mac.init(new SecretKeySpec(publicKeys, "AES"));
    } catch (InvalidKeyException e) {
      Log.e(TAG, "Error initializing SHA256 HMAC instance", e);
      return null;
    }

    byte[] sharedSecret =getSharedSecret();
    if (sharedSecret == null) {
      Log.e(TAG, "Shared secret is zero. Possibly indicates a weak public key!");
      return null;
    }

    Log.v("TAG", "beacon shared secret: " + bytesToHex(sharedSecret, 0, 32, false));

    sharedSecret = hkdfSha256Mac.doFinal(getSharedSecret());
    if (sharedSecret == null) {
      Log.e(TAG, "Shared secret is zero. Possibly indicates a weak public key!");
      return null;
    }

    Log.v("TAG", "beacon final shared secret: " + bytesToHex(sharedSecret, 0, 32, false));

    try {
      hkdfSha256Mac.init(new SecretKeySpec(sharedSecret, "AES"));
    } catch (InvalidKeyException e) {
      Log.e(TAG, "Error reinitializing SHA256 HMAC instance", e);
      return null;
    }

    byte[] salt = { 0x01 };
    return Arrays.copyOfRange(hkdfSha256Mac.doFinal(salt), 0, 16);
  }

  byte[] generateTkData(int timestampSeconds) {
    // TODO: link to docs showing encryption data structure.
    byte[] data = new byte[16];
    data[11] = (byte) 0xff;  // Salt.
    int trimmedTime = timestampSeconds >> 16;
    data[14] = (byte) ((trimmedTime >> 8) & 0xff);
    data[15] = (byte) (trimmedTime & 0xff);
    return data;
  }

  byte[] getTemporaryKey(int timestampSeconds) {
    byte[] tkData = generateTkData(timestampSeconds);
    byte[] identityKey = getIdentityKey();
    return identityKey == null ? null
        : aes128Encrypt(tkData, new SecretKeySpec(identityKey, "AES"));
  }

  byte[] generateEidrData(int rotationPeriodExponent, int timestampSeconds) {
    // TODO: link to docs showing encryption data structure.
    byte[] data = new byte[16];
    data[11] = (byte) rotationPeriodExponent;
    // Clear the {@code rotationPeriodExponent} bits of the timestamp.
    long scaledTime = (timestampSeconds >> rotationPeriodExponent) << rotationPeriodExponent;
    data[12] = ((byte) ((scaledTime >> 24) & 0xff));
    data[13] = ((byte) ((scaledTime >> 16) & 0xff));
    data[14] = ((byte) ((scaledTime >> 8) & 0xff));
    data[15] = ((byte) (scaledTime & 0xff));
    return data;
  }

  private byte[] getEidrInternal(int rotationPeriodExponent, int timestampSeconds) {
    byte[] temporaryKey = getTemporaryKey(timestampSeconds);
    if (temporaryKey == null) {
      return null;
    }
    byte[] eidData = generateEidrData(rotationPeriodExponent, timestampSeconds);
    byte[] encrypted =  aes128Encrypt(eidData, new SecretKeySpec(temporaryKey, "AES"));
    return encrypted == null ? null : Arrays.copyOfRange(encrypted, 0, 8);
  }

  public byte[] aes128Encrypt(byte[] data, SecretKeySpec keySpec) {
    Cipher cipher;
    try {
      // Ignore the "ECB encryption should not be used" warning. We use exactly one block so
      // the difference between ECB and CBC is just an IV or not. In addition our blocks are
      // always different since they have a monotonic timestamp. Most importantly, our blocks
      // aren't sensitive. Decrypting them means means knowing the beacon time and its rotation
      // period. If due to ECB an attacker could find out that the beacon broadcast the same
      // block a second time, all it could infer is that for some reason the clock of the beacon
      // reset, which is not very helpful
      cipher = Cipher.getInstance("AES/ECB/NoPadding");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      Log.e(TAG, "Error constructing cipher instance", e);
      return null;
    }

    try {
      cipher.init(Cipher.ENCRYPT_MODE, keySpec);
    } catch (InvalidKeyException e) {
      Log.e(TAG, "Error initializing cipher instance", e);
      return null;
    }

    byte[] ret;
    try {
      ret = cipher.doFinal(data);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      Log.e(TAG, "Error executing cipher", e);
      return null;
    }

    return ret;
  }

  private void checkArgument(boolean b) {
    if (!b) {
      throw new IllegalArgumentException();
    }
  }

  void setPrivateKeyForTesting(byte[] b) {
    this.beaconPrivateKey = b;
    this.beaconPublicKey = Curve25519.scalarMultBase(b);
  }
}
