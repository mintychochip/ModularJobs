package net.aincraft.boost;

import dev.databag.DataHandler;
import net.kyori.adventure.key.Key;

/**
 * DataBag codec for item boost JSON ({@code modularjobs:boost_data}, format 1).
 */
public final class BoostPayloadHandler implements DataHandler<byte[]> {

  public static final Key KEY = Key.key("modularjobs", "boost_data");
  public static final int FORMAT = 1;
  public static final BoostPayloadHandler INSTANCE = new BoostPayloadHandler();

  private BoostPayloadHandler() {}

  @Override
  public Key key() {
    return KEY;
  }

  @Override
  public int format() {
    return FORMAT;
  }

  @Override
  public byte[] encode(byte[] value) {
    return value.clone();
  }

  @Override
  public byte[] decode(byte[] bytes) {
    return bytes.clone();
  }
}
