package net.aincraft.serialization;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.kyori.adventure.key.Key;

final class BinaryOutImpl implements BinaryOut {

  private byte[] buffer;
  private int position;

  BinaryOutImpl(int initialCapacity) {
    this.buffer = new byte[Math.max(16, initialCapacity)];
  }

  @Override
  public void writeByte(byte v) {
    ensure(1);
    buffer[position++] = v;
  }

  @Override
  public void writeBool(boolean b) {
    ensure(1);
    buffer[position++] = (byte) (b ? 1 : 0);
  }

  @Override
  public void writeChar(char c) {
    ensure(2);
    buffer[position++] = (byte) (c >>> 8);
    buffer[position++] = (byte) c;
  }

  @Override
  public void writeUnsignedInt(int v) {
    while ((v & ~0x7F) != 0) {
      writeByte((byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    writeByte((byte) v);
  }

  @Override
  public void writeInt(int v) {
    v = (v << 1) ^ (v >> 31);
    while ((v & ~0x7F) != 0) {
      writeByte((byte) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    writeByte((byte) (v & 0x7F));
  }

  @Override
  public void writeFixedInt(int v) {
    ensure(4);
    buffer[position++] = (byte) (v >>> 24);
    buffer[position++] = (byte) (v >>> 16);
    buffer[position++] = (byte) (v >>> 8);
    buffer[position++] = (byte) v;
  }

  @Override
  public void writeFixedLong(long v) {
    ensure(8);
    buffer[position++] = (byte) (v >>> 56);
    buffer[position++] = (byte) (v >>> 48);
    buffer[position++] = (byte) (v >>> 40);
    buffer[position++] = (byte) (v >>> 32);
    buffer[position++] = (byte) (v >>> 24);
    buffer[position++] = (byte) (v >>> 16);
    buffer[position++] = (byte) (v >>> 8);
    buffer[position++] = (byte) v;
  }

  @Override
  public void writeLong(long v) {
    long zz = (v << 1) ^ (v >> 63);
    while ((zz & ~0x7FL) != 0) {
      writeByte((byte) ((zz & 0x7F) | 0x80));
      zz >>>= 7;
    }
    writeByte((byte) zz);
  }

  @Override
  public void writeFloat(float v) {
    writeInt(Float.floatToIntBits(v));
  }

  @Override
  public void writeDouble(double v) {
    writeLong(Double.doubleToLongBits(v));
  }

  @Override
  public void writeString(String s) {
    byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
    writeInt(utf8.length);
    writeBytes(utf8);
  }

  @Override
  public void writeBytes(byte[] v) {
    ensure(v.length);
    System.arraycopy(v, 0, buffer, position, v.length);
    position += v.length;
  }

  @Override
  public void writeBigDecimal(BigDecimal value) {
    BigDecimal n = value.stripTrailingZeros();
    if (n.signum() == 0) {
      writeInt(0);
      writeInt(0);
      return;
    }
    int scale = n.scale();
    byte[] mag = n.unscaledValue().toByteArray();
    writeInt(scale);
    writeInt(mag.length);
    writeBytes(mag);
  }

  @Override
  public <E extends Enum<E>> void writeEnum(E enumMember) {
    int ordinal = enumMember.ordinal();
    writeInt(ordinal);
  }

  @Override
  public void writeKey(Key key) {
    String namespace = key.namespace();
    boolean minecraft = "minecraft".equals(namespace);
    writeByte((byte) (minecraft ? 1 : 0));
    if (!minecraft) {
      writeString(namespace);
    }
    writeString(key.value());
  }

  @Override
  public byte[] toByteArray() {
    return Arrays.copyOf(buffer, position);
  }

  private void ensure(int add) {
    int required = position + add;
    if (required > buffer.length) {
      int newCapacity = Math.max(buffer.length << 1, required);
      buffer = Arrays.copyOf(buffer, newCapacity);
    }
  }
}
