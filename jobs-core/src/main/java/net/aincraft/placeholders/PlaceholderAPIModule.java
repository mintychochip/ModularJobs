package net.aincraft.placeholders;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public final class PlaceholderAPIModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(PlaceholderExpansion.class).to(ModularJobsPlaceholderExpansion.class).in(Singleton.class);
  }
}
