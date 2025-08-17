package net.aincraft.serialization;

import java.math.BigDecimal;
import net.kyori.adventure.key.Key;

public interface BinaryIn {

  byte readByte();

  boolean readBool();

  char readChar();

  int readUnsignedInt();

  int readInt();

  int readFixedInt();

  long readFixedLong();

  long readLong();

  float readFloat();

  double readDouble();

  String readString();

  byte[] readBytes(int length);

  BigDecimal readBigDecimal();

  <E extends Enum<E>> E readEnum(Class<E> enumClass);

  Key readKey();

  boolean hasRemaining();
}
