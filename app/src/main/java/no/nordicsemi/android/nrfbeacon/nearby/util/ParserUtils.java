/*************************************************************************************************************************************************
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************************************************************************************/
package no.nordicsemi.android.nrfbeacon.nearby.util;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.webkit.URLUtil;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class ParserUtils {
    private static final char[] HEX_ARRAY = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    public static final int FORMAT_UINT24 = 0x13;
    public static final int FORMAT_SINT24 = 0x23;
    public static final int FORMAT_UINT16_BIG_INDIAN = 0x62;
    public static final int FORMAT_UINT32_BIG_INDIAN = 0x64;


    /**
     * URI Scheme maps a byte code into the scheme and an optional scheme specific prefix.
     */
    private static final SparseArray<String> URI_SCHEMES = new SparseArray<String>() {
        {
            put((byte) 0, "http://www.");
            put((byte) 1, "https://www.");
            put((byte) 2, "http://");
            put((byte) 3, "https://");
            put((byte) 4, "urn:uuid:"); // RFC 2141 and RFC 4122};
        }
    };

    /**
     * Expansion strings for "http" and "https" schemes. These contain strings appearing anywhere in a
     * URL. Restricted to Generic TLDs.
     * <p/>
     * Note: this is a scheme specific encoding.
     */
    private static final SparseArray<String> URL_CODES = new SparseArray<String>() {
        {
            put((byte) 0, ".com/");
            put((byte) 1, ".org/");
            put((byte) 2, ".edu/");
            put((byte) 3, ".net/");
            put((byte) 4, ".info/");
            put((byte) 5, ".biz/");
            put((byte) 6, ".gov/");
            put((byte) 7, ".com");
            put((byte) 8, ".org");
            put((byte) 9, ".edu");
            put((byte) 10, ".net");
            put((byte) 11, ".info");
            put((byte) 12, ".biz");
            put((byte) 13, ".gov");
        }
    };
    private static final String TAG = "MCP";

    public static String bytesToHex(final byte[] bytes, final boolean add0x) {
        if (bytes == null)
            return "";
        return bytesToHex(bytes, 0, bytes.length, add0x);
    }

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

    public static String bytesToAddress(final byte[] bytes, final int start) {
        if (bytes == null || bytes.length < start + 6)
            return "";

        final int maxLength = 6;
        final char[] hexChars = new char[maxLength * 3 - 1];
        for (int j = 0; j < maxLength; j++) {
            final int v = bytes[start + j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            if (j < maxLength - 1)
                hexChars[j * 3 + 2] = ':';
        }
        return new String(hexChars);
    }

    public static UUID bytesToUUID(final byte[] bytes, final int start, final int length) {
        if (bytes == null || bytes.length < start + 16 || length != 16)
            return null;

        long msb = 0L;
        long lsb = 0L;
        for (int i = 0; i < 8; ++i)
            msb += (bytes[start + i] & 0xFFL) << (56 - i * 8);
        for (int i = 0; i < 8; ++i)
            lsb += (bytes[start + i + 8] & 0xFFL) << (56 - i * 8);

        return new UUID(msb, lsb);
    }

    public static String deviceTypeTyString(final int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "CLASSIC";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "CLASSIC and BLE";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "BLE only";
            default:
                return "UNKNOWN";
        }
    }

    public static String bondingStateToString(final int state) {
        switch (state) {
            case BluetoothDevice.BOND_BONDING:
                return "BONDING...";
            case BluetoothDevice.BOND_BONDED:
                return "BONDED";
            default:
                return "NOT BONDED";
        }
    }

    public static String getProperties(final BluetoothGattCharacteristic characteristic) {
        final int properties = characteristic.getProperties();
        final StringBuilder builder = new StringBuilder();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0)
            builder.append("B ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) > 0)
            builder.append("E ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0)
            builder.append("I ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
            builder.append("N ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0)
            builder.append("R ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) > 0)
            builder.append("SW ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
            builder.append("W ");
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0)
            builder.append("WNR ");

        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
            builder.insert(0, "[");
            builder.append("]");
        }
        return builder.toString();
    }

    public static int setValue(final byte[] dest, int offset, int value, int formatType) {
        int len = offset + getTypeLen(formatType);
        if (len > dest.length)
            return offset;

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_SINT8:
                value = intToSignedBits(value, 8);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT8:
                dest[offset] = (byte) (value & 0xFF);
                break;

            case BluetoothGattCharacteristic.FORMAT_SINT16:
                value = intToSignedBits(value, 16);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT16:
                dest[offset++] = (byte) (value & 0xFF);
                dest[offset] = (byte) ((value >> 8) & 0xFF);
                break;

            case FORMAT_SINT24:
                value = intToSignedBits(value, 24);
                // Fall-through intended
            case FORMAT_UINT24:
                dest[offset++] = (byte) (value & 0xFF);
                dest[offset++] = (byte) ((value >> 8) & 0xFF);
                dest[offset] = (byte) ((value >> 16) & 0xFF);
                break;

            case FORMAT_UINT16_BIG_INDIAN:
                dest[offset++] = (byte) ((value >> 8) & 0xFF);
                dest[offset] = (byte) (value & 0xFF);
                break;

            case BluetoothGattCharacteristic.FORMAT_SINT32:
                value = intToSignedBits(value, 32);
                // Fall-through intended
            case BluetoothGattCharacteristic.FORMAT_UINT32:
                dest[offset++] = (byte) (value & 0xFF);
                dest[offset++] = (byte) ((value >> 8) & 0xFF);
                dest[offset++] = (byte) ((value >> 16) & 0xFF);
                dest[offset] = (byte) ((value >> 24) & 0xFF);
                break;

            case FORMAT_UINT32_BIG_INDIAN:
                dest[offset++] = (byte) ((value >> 24) & 0xFF);
                dest[offset++] = (byte) ((value >> 16) & 0xFF);
                dest[offset++] = (byte) ((value >> 8) & 0xFF);
                dest[offset] = (byte) (value & 0xFF);
                break;

            default:
                return offset;
        }
        return len;
    }

    public static int setValue(final byte[] dest, int offset, int mantissa, int exponent, int formatType) {
        int len = offset + getTypeLen(formatType);
        if (len > dest.length)
            return offset;

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                mantissa = intToSignedBits(mantissa, 12);
                exponent = intToSignedBits(exponent, 4);
                dest[offset++] = (byte) (mantissa & 0xFF);
                dest[offset] = (byte) ((mantissa >> 8) & 0x0F);
                dest[offset] += (byte) ((exponent & 0x0F) << 4);
                break;

            case BluetoothGattCharacteristic.FORMAT_FLOAT:
                mantissa = intToSignedBits(mantissa, 24);
                exponent = intToSignedBits(exponent, 8);
                dest[offset++] = (byte) (mantissa & 0xFF);
                dest[offset++] = (byte) ((mantissa >> 8) & 0xFF);
                dest[offset++] = (byte) ((mantissa >> 16) & 0xFF);
                dest[offset] += (byte) (exponent & 0xFF);
                break;

            default:
                return offset;
        }

        return len;
    }

    public static int setValue(final byte[] dest, final int offset, final String value) {
        if (value == null)
            return offset;

        final byte[] valueBytes = value.getBytes();
        System.arraycopy(valueBytes, 0, dest, offset, valueBytes.length);
        return offset + valueBytes.length;
    }

    public static int setByteArrayValue(final byte[] dest, final int offset, final String value) {
        if (value == null)
            return offset;

        for (int i = 0; i < value.length(); i += 2) {
            dest[offset + i / 2] = (byte) ((Character.digit(value.charAt(i), 16) << 4)
                    + Character.digit(value.charAt(i + 1), 16));
        }
        return offset + value.length() / 2;
    }

    public static int getIntValue(final byte[] source, final int offset, final int formatType) {
        if ((offset + getTypeLen(formatType)) > source.length)
            throw new ArrayIndexOutOfBoundsException();

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_UINT8:
                return unsignedByteToInt(source[offset]);

            case BluetoothGattCharacteristic.FORMAT_UINT16:
                return unsignedBytesToInt(source[offset], source[offset + 1]);

            case FORMAT_UINT24:
                return unsignedBytesToInt(source[offset], source[offset + 1], source[offset + 2]);

            case BluetoothGattCharacteristic.FORMAT_UINT32:
                return unsignedBytesToInt(source[offset], source[offset + 1], source[offset + 2], source[offset + 3]);

            case FORMAT_UINT16_BIG_INDIAN:
                return unsignedBytesToInt(source[offset + 1], source[offset]);

            case FORMAT_UINT32_BIG_INDIAN:
                return unsignedBytesToInt(source[offset + 3], source[offset + 2], source[offset + 1], source[offset]);

            case BluetoothGattCharacteristic.FORMAT_SINT8:
                return unsignedToSigned(unsignedByteToInt(source[offset]), 8);

            case BluetoothGattCharacteristic.FORMAT_SINT16:
                return unsignedToSigned(unsignedBytesToInt(source[offset], source[offset + 1]), 16);

            case FORMAT_SINT24:
                return unsignedToSigned(unsignedBytesToInt(source[offset], source[offset + 1], source[offset + 2]), 24);

            case BluetoothGattCharacteristic.FORMAT_SINT32:
                return unsignedToSigned(unsignedBytesToInt(source[offset], source[offset + 1], source[offset + 2], source[offset + 3]), 32);
        }
        return 0;
    }

    public static int getMantissa(final byte[] source, final int offset, final int formatType) {
        if ((offset + getTypeLen(formatType)) > source.length)
            throw new ArrayIndexOutOfBoundsException();

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                return unsignedToSigned(unsignedByteToInt(source[offset]) + ((unsignedByteToInt(source[offset + 1]) & 0x0F) << 8), 12);
            case BluetoothGattCharacteristic.FORMAT_FLOAT:
                return unsignedToSigned(unsignedByteToInt(source[offset]) + (unsignedByteToInt(source[offset + 1]) << 8) + (unsignedByteToInt(source[offset + 2]) << 16), 24);
        }
        return 0;
    }

    public static int getExponent(final byte[] source, final int offset, final int formatType) {
        if ((offset + getTypeLen(formatType)) > source.length)
            throw new ArrayIndexOutOfBoundsException();

        switch (formatType) {
            case BluetoothGattCharacteristic.FORMAT_SFLOAT:
                return unsignedToSigned(unsignedByteToInt(source[offset + 1]) >> 4, 4);
            case BluetoothGattCharacteristic.FORMAT_FLOAT:
                return source[offset + 3];
        }
        return 0;
    }

    /**
     * Returns the size of a give value type.
     */
    public static int getTypeLen(int formatType) {
        return formatType & 0xF;
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    private static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    private static int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    /**
     * Convert signed bytes to a 24-bit unsigned int.
     */
    private static int unsignedBytesToInt(byte b0, byte b1, byte b2) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8)) + (unsignedByteToInt(b2) << 16);
    }

    /**
     * Convert signed bytes to a 32-bit unsigned int.
     */
    private static int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8)) + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded
     * signed value.
     */
    private static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

    /**
     * Convert an integer into the signed bits of a given length.
     */
    private static int intToSignedBits(int i, int size) {
        if (i < 0) {
            i = (1 << size - 1) + (i & ((1 << size - 1) - 1));
        }
        return i;
    }

    public static int decodeUuid16(final byte[] data, final int start) {
        final int b1 = data[start] & 0xff;
        final int b2 = data[start + 1] & 0xff;

        return (b2 << 8 | b1) & 0xFFFF;
    }

    public static int decodeUuid32(final byte[] data, final int start) {
        final int b1 = data[start] & 0xff;
        final int b2 = data[start + 1] & 0xff;
        final int b3 = data[start + 2] & 0xff;
        final int b4 = data[start + 3] & 0xff;

        return b4 << 24 | b3 << 16 | b2 << 8 | b1;
    }

    public static int intOrThrow(final Integer i) {
        if (i == null)
            throw new NullPointerException();
        return i;
    }

    public static byte[] base64Decode(String s) {
        return Base64.decode(s, Base64.DEFAULT);
    }

    public static String base64Encode(byte[] b) {
        return Base64.encodeToString(b, Base64.DEFAULT).trim();
    }

    public static String base64Encode(String s) {
        return base64Encode(setByteArrayValue(s));
    }

    private static byte[] setByteArrayValue(String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    public static String decodeUri(final byte[] serviceData, final int start, final int length) {
        if (start < 0 || serviceData.length < start + length)
            return null;

        final StringBuilder uriBuilder = new StringBuilder();
        int offset = 0;
        if (offset < length) {
            byte b = serviceData[start + offset++];
            String scheme = URI_SCHEMES.get(b);
            if (scheme != null) {
                uriBuilder.append(scheme);
                if (URLUtil.isNetworkUrl(scheme)) {
                    return decodeUrl(serviceData, start + offset, length - 1, uriBuilder);
                } else if ("urn:uuid:".equals(scheme)) {
                    return decodeUrnUuid(serviceData, start + offset, uriBuilder);
                }
            }
            Log.w(TAG, "decodeUri unknown Uri scheme code=" + b);
        }
        return null;
    }

    private static String decodeUrl(final byte[] serviceData, final int start, final int length, final StringBuilder urlBuilder) {
        int offset = 0;
        while (offset < length) {
            byte b = serviceData[start + offset++];
            String code = URL_CODES.get(b);
            if (code != null) {
                urlBuilder.append(code);
            } else {
                urlBuilder.append((char) b);
            }
        }
        return urlBuilder.toString();
    }

    private static String decodeUrnUuid(final byte[] serviceData, final int offset, final StringBuilder urnBuilder) {
        ByteBuffer bb = ByteBuffer.wrap(serviceData);
        // UUIDs are ordered as byte array, which means most significant first
        bb.order(ByteOrder.BIG_ENDIAN);
        long mostSignificantBytes, leastSignificantBytes;
        try {
            bb.position(offset);
            mostSignificantBytes = bb.getLong();
            leastSignificantBytes = bb.getLong();
        } catch (BufferUnderflowException e) {
            Log.w(TAG, "decodeUrnUuid BufferUnderflowException!");
            return null;
        }
        UUID uuid = new UUID(mostSignificantBytes, leastSignificantBytes);
        urnBuilder.append(uuid.toString());
        return urnBuilder.toString();
    }

    /**
     * Creates the Uri string with embedded expansion codes.
     *
     * @param uri to be encoded
     * @return the Uri string with expansion codes.
     */
    public static byte[] encodeUri(String uri) {
        if (uri.length() == 0) {
            return new byte[0];
        }
        ByteBuffer bb = ByteBuffer.allocate(uri.length());
        // UUIDs are ordered as byte array, which means most significant first
        bb.order(ByteOrder.BIG_ENDIAN);
        int position = 0;

        // Add the byte code for the scheme or return null if none
        Byte schemeCode = encodeUriScheme(uri);
        if (schemeCode == null) {
            return null;
        }
        String scheme = URI_SCHEMES.get(schemeCode);
        bb.put(schemeCode);
        position += scheme.length();

        if (URLUtil.isNetworkUrl(scheme)) {
            return encodeUrl(uri, position, bb);
        } else if ("urn:uuid:".equals(scheme)) {
            return encodeUrnUuid(uri, position, bb);
        }
        return null;
    }
    private static Byte encodeUriScheme(String uri) {
        String lowerCaseUri = uri.toLowerCase(Locale.ENGLISH);
        for (int i = 0; i < URI_SCHEMES.size(); i++) {
            // get the key and value.
            int key = URI_SCHEMES.keyAt(i);
            String value = URI_SCHEMES.valueAt(i);
            if (lowerCaseUri.startsWith(value)) {
                return (byte) key;
            }
        }
        return null;
    }

    private static byte[] encodeUrl(String url, int position, ByteBuffer bb) {
        while (position < url.length()) {
            byte expansion = findLongestExpansion(url, position);
            if (expansion >= 0) {
                bb.put(expansion);
                position += URL_CODES.get(expansion).length();
            } else {
                bb.put((byte) url.charAt(position++));
            }
        }
        return byteBufferToArray(bb);
    }

    private static byte[] encodeUrnUuid(String urn, int position, ByteBuffer bb) {
        String uuidString = urn.substring(position, urn.length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "encodeUrnUuid invalid urn:uuid format - " + urn);
            return null;
        }
        // UUIDs are ordered as byte array, which means most significant first
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return byteBufferToArray(bb);
    }

    private static byte[] byteBufferToArray(ByteBuffer bb) {
        byte[] bytes = new byte[bb.position()];
        bb.rewind();
        bb.get(bytes, 0, bytes.length);
        return bytes;
    }

    /**
     * Finds the longest expansion from the uri at the current position.
     *
     * @param uriString the Uri
     * @param pos start position
     * @return an index in URI_MAP or 0 if none.
     */
    private static byte findLongestExpansion(String uriString, int pos) {
        byte expansion = -1;
        int expansionLength = 0;
        for (int i = 0; i < URL_CODES.size(); i++) {
            // get the key and value.
            int key = URL_CODES.keyAt(i);
            String value = URL_CODES.valueAt(i);
            if (value.length() > expansionLength && uriString.startsWith(value, pos)) {
                expansion = (byte) key;
                expansionLength = value.length();
            }
        }
        return expansion;
    }

    public static byte[] toByteArray(String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    public static int decodeUint16BigEndian(final byte[] data, final int start) {
        final int b1 = data[start] & 0xff;
        final int b2 = data[start + 1] & 0xff;

        return b1 << 8 | b2;
    }

    /**
     * This method returns the Uint32 value encoded with Big Endian.
     */
    public static long decodeUint32BigEndian(final byte[] data, final int start) {
        final int b1 = data[start] & 0xff;
        final int b2 = data[start + 1] & 0xff;
        final int b3 = data[start + 2] & 0xff;
        final int b4 = data[start + 3] & 0xff;

        return (b1 << 24 | b2 << 16 | b3 << 8 | b4) & 0xFFFFFFFFL;
    }

    public static float decode88FixedPointNotation(final byte[] data, final int start) {
        return data[start] + (float) (data[start + 1] & 0xFF) / 256.f;
    }

    public static String randomUid(int len) {
        byte[] buf = new byte[len];
        new Random().nextBytes(buf);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            stringBuilder.append(String.format("%02x", buf[i]));
        }
        return stringBuilder.toString();
    }

    public static byte[] aes128Encrypt(byte[] data, SecretKeySpec keySpec) {
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

    public static byte[] aes128decrypt(byte[] data, SecretKeySpec keySpec) {
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
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
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

    public static String parse(final byte[] bytes, final int offset, final int length, final String unit) {
        final String notNullUnit = unit == null ? "" : " " + unit;

        switch (length) {
            case 1:
                return String.valueOf(ParserUtils.getIntValue(bytes, offset, BluetoothGattCharacteristic.FORMAT_SINT8)) + notNullUnit;
            case 2:
                return String.valueOf(ParserUtils.getIntValue(bytes, offset, BluetoothGattCharacteristic.FORMAT_SINT16)) + notNullUnit;
            case 3:
                return String.valueOf(ParserUtils.getIntValue(bytes, offset, ParserUtils.FORMAT_SINT24)) + notNullUnit;
            case 4:
                return String.valueOf(ParserUtils.getIntValue(bytes, offset, BluetoothGattCharacteristic.FORMAT_SINT32)) + notNullUnit;
            case 16:
                return ParserUtils.bytesToHex(bytes, offset, length, true);
        }
        return "Invalid data syntax: " + ParserUtils.bytesToHex(bytes, offset, length, true);
    }
}