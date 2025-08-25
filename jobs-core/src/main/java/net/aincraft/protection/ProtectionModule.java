package net.aincraft.protection;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public final class ProtectionModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BlockOwnershipService.class).to(BlockOwnershipServiceImpl.class).in(Singleton.class);
    bind(BlockProtectionAdapter.class).toProvider(BlockProtectionAdapterProvider.class)
        .in(Singleton.class);
  }
}
