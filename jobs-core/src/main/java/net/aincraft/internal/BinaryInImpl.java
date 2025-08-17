package net.aincraft.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.aincraft.serialization.BinaryIn;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

public final class BinaryInImpl implements BinaryIn {

  private final byte[] buffer;
  private int position;

  public BinaryInImpl(byte[] data) {
    buffer = data;
    position = 0;
  }

  @Override
  public byte readByte() {
    return buffer[position++];
  }

  @Override
  public boolean readBool() {
    return readByte() != 0;
  }

  @Override
  public char readChar() {
    int high = readByte() & 0xFF;
    int low = readByte() & 0xFF;
    return (char) ((high << 8) | low);
  }

  @Override
  public int readUnsignedInt() {
    int result = 0, shift = 0;
    while (true) {
      int b = readByte() & 0xFF;
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        break;
      }
      shift += 7;
      if (shift > 35) {
        throw new IllegalArgumentException("uvar int too long");
      }
    }
    return result;
  }

  @Override
  public int readInt() {
    int result = 0;
    int shift = 0;
    while (true) {
      int b = readByte() & 0xFF;
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        break;
      }
      shift += 7;
      if (shift > 35) {
        throw new IllegalArgumentException("var int too long");
      }
    }
    return (result >>> 1) ^ -(result & 1);
  }

  @Override
  public int readFixedInt() {
    return (readByte() & 0xFF) << 24
        | (readByte() & 0xFF) << 16
        | (readByte() & 0xFF) << 8
        | (readByte() & 0xFF);
  }

  @Override
  public long readFixedLong() {
    return ((long) (readByte() & 0xFF) << 56)
        | ((long) (readByte() & 0xFF) << 48)
        | ((long) (readByte() & 0xFF) << 40)
        | ((long) (readByte() & 0xFF) << 32)
        | ((long) (readByte() & 0xFF) << 24)
        | ((long) (readByte() & 0xFF) << 16)
        | ((long) (readByte() & 0xFF) << 8)
        | ((long) (readByte() & 0xFF));
  }

  @Override
  public long readLong() {
    long result = 0;
    int shift = 0;
    while (true) {
      int b = readByte() & 0xFF;
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        break;
      }
      shift += 7;
      if (shift > 70) {
        throw new IllegalArgumentException("var long too long");
      }
    }
    return (result >>> 1) ^ -(result & 1);
  }

  @Override
  public float readFloat() {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public double readDouble() {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public String readString() {
    int length = readInt();
    byte[] utf8 = readBytes(length);
    return new String(utf8, StandardCharsets.UTF_8);
  }

  @Override
  public byte[] readBytes(int length) {
    byte[] result = Arrays.copyOfRange(buffer, position, position + length);
    position += length;
    return result;
  }

  @Override
  public BigDecimal readBigDecimal() {
    int scale = readInt();
    int len = readInt();
    if (len == 0) {
      return BigDecimal.ZERO;
    }
    byte[] mag = readBytes(len);
    return new BigDecimal(new BigInteger(mag), scale);
  }

  @Override
  public <E extends Enum<E>> E readEnum(Class<E> enumClass) {
    int ordinal = readInt();
    return enumClass.getEnumConstants()[ordinal];
  }

  @Override
  public Key readKey() {
    byte minecraft = readByte();
    String namespace = minecraft == 1 ? "minecraft" : readString();
    return new NamespacedKey(namespace, readString());
  }

  @Override
  public boolean hasRemaining() {
    return position < buffer.length;
  }
}
