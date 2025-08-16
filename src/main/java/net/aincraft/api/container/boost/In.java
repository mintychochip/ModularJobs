package net.aincraft.api.container.boost;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

//TODO: add checks to reads guys
public final class In {

  private final byte[] buffer;
  private int position;

  public In(byte[] data) {
    buffer = data;
    position = 0;
  }

  public int readByte() {
    return buffer[position++] & 0xFF;
  }

  public boolean readBool() {
    return readByte() != 0;
  }

  public int readInt() {
    return (readByte() << 24)
        | (readByte() << 16)
        | (readByte() << 8)
        | readByte();
  }

  public long readLong() {
    return ((long) readByte() << 56)
        | ((long) readByte() << 48)
        | ((long) readByte() << 40)
        | ((long) readByte() << 32)
        | ((long) readByte() << 24)
        | ((long) readByte() << 16)
        | ((long) readByte() << 8)
        | ((long) readByte());
  }

  public char readChar() {
    int high = readByte();
    int low = readByte();
    return (char) ((high << 8) | low);
  }

  public float readFloat() {
    return Float.intBitsToFloat(readInt());
  }

  public double readDouble() {
    return Double.longBitsToDouble(readLong());
  }

  public String readString() {
    int length = readInt();
    byte[] utf8 = readBytes(length);
    return new String(utf8, StandardCharsets.UTF_8);
  }

  public <E extends Enum<E>> E readEnum(Class<E> enumClass) throws IllegalArgumentException {
    String raw = readString();
    return Enum.valueOf(enumClass, raw);
  }

  public Key readKey() {
    String namespace = readString();
    String value = readString();
    return new NamespacedKey(namespace,value);
  }

  public byte[] readBytes(int length) {
    byte[] result = Arrays.copyOfRange(buffer, position, position + length);
    position += length;
    return result;
  }
}
