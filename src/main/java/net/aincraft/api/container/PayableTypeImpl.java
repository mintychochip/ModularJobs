package net.aincraft.api.container;

import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

record PayableTypeImpl(@NotNull PayableHandler handler,
                                                @NotNull Key key) implements PayableType {

  @Internal
  static PayableType create(PayableHandler handler, Key key) {
    PayableTypeImpl type = new PayableTypeImpl(handler, key);
    RegistryContainer.registryContainer()
        .editRegistry(RegistryKeys.PAYABLE_TYPES, r -> r.register(type));
    return type;
  }

  @Override
  public Payable create(PayableAmount amount) {
    return Payable.create(this,amount);
  }
}
