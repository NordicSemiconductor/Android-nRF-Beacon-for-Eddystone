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

package eidr;

import android.util.Log;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * A sample implementation of Eddystone EIDR computation.
 */
public class EddystoneEidrGenerator {
  private static final String TAG = EddystoneEidrGenerator.class.getSimpleName();

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
   * Returns the beacon's public key.
   */
  public byte[] generateBeaconPublicKey() {
    return Curve25519.scalarMultBase(beaconPrivateKey);
  }

  byte[] getSharedSecret() {
    return Curve25519.scalarMult(beaconPrivateKey, serviceEcdhPublicKey);
  }

  public byte[] getIdentityKey() {
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

    byte[] sharedSecret = hkdfSha256Mac.doFinal(getSharedSecret());
    if (sharedSecret == null) {
      Log.e(TAG, "Shared secret is zero. Possibly indicates a weak public key!");
      return null;
    }

    try {
      hkdfSha256Mac.init(new SecretKeySpec(sharedSecret, "AES"));
    } catch (InvalidKeyException e) {
      Log.e(TAG, "Error reinitializing SHA256 HMAC instance", e);
      return null;
    }

    byte[] salt = { 0x01 };
    return Arrays.copyOfRange(hkdfSha256Mac.doFinal(salt), 0, 16);
  }

  private void checkArgument(boolean b) {
    if (!b) {
      throw new IllegalArgumentException();
    }
  }
}
