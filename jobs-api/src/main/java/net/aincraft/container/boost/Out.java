package net.aincraft.container.boost;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.kyori.adventure.key.Key;

public class Out {

  private byte[] buffer;
  private int position;

  public Out(int initialCapacity) {
    buffer = new byte[Math.max(16, initialCapacity)];
  }

  public int position() {
    return position;
  }

  public void writeByte(byte v) {
    ensure(1);
    buffer[position++] = v;
  }

  public void writeBool(boolean b) {
    ensure(1);
    buffer[position++] = (byte) (b ? 1 : 0);
  }

  public void writeChar(char c) {
    ensure(2);
    buffer[position++] = (byte) (c >>> 8);
    buffer[position++] = (byte) c;
  }

  public void writeInt(int v) {
    ensure(4);
    buffer[position++] = (byte) (v >>> 24);
    buffer[position++] = (byte) (v >>> 16);
    buffer[position++] = (byte) (v >>> 8);
    buffer[position++] = (byte) v;
  }

  public void writeLong(long v) {
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

  public void writeFloat(float v) {
    writeInt(Float.floatToIntBits(v));
  }

  public void writeDouble(double v) {
    writeLong(Double.doubleToLongBits(v));
  }

  public void writeString(String s) {
    byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
    writeInt(utf8.length); // length prefix
    writeBytes(utf8);
  }

  public void writeBytes(byte[] src) {
    ensure(src.length);
    System.arraycopy(src, 0, buffer, position, src.length);
    position += src.length;
  }

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

  public byte[] toByteArray() {
    return Arrays.copyOf(buffer, position);
  }

  public <E extends Enum<E>> void writeEnum(E enumMember) {
    writeString(enumMember.toString());
  }

  public void writeKey(Key key) {
    writeString(key.namespace());
    writeString(key.value());
  }

  private void ensure(int add) {
    int required = position + add;
    if (required > buffer.length) {
      int newCapacity = Math.max(buffer.length << 1, required);
      buffer = Arrays.copyOf(buffer, newCapacity);
    }
  }
}
