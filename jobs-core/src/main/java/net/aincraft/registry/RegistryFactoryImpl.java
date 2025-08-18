package net.aincraft.registry;

import net.kyori.adventure.key.Keyed;

final class RegistryFactoryImpl implements RegistryFactory{

  @Override
  public <T extends Keyed> Registry<T> simple() {
    return new SimpleRegistryImpl<>();
  }
}
