package net.aincraft.serialization;

import java.math.BigDecimal;
import net.kyori.adventure.key.Key;

public interface BinaryOut {

  void writeByte(byte v);

  void writeBool(boolean b);

  void writeChar(char c);

  void writeUnsignedInt(int v);

  void writeInt(int v);

  void writeFixedInt(int v);

  void writeFixedLong(long v);

  void writeLong(long v);

  void writeFloat(float v);

  void writeDouble(double v);

  void writeString(String s);

  void writeBytes(byte[] v);

  void writeBigDecimal(BigDecimal v);

  <E extends Enum<E>> void writeEnum(E enumMember);

  void writeKey(Key key);

  byte[] toByteArray();
}
