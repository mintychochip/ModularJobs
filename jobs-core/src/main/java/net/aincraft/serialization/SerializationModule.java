package net.aincraft.serialization;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public final class SerializationModule extends AbstractModule {

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public CodecRegistry codecRegistry() {
    return new KryoCodecRegistry();
  }
}
