package net.aincraft.api.action;

import java.util.Locale;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.ItemContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.context.KeyResolver.KeyResolvingStrategy;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

record ActionTypeImpl(Key key) implements ActionType {

  static {KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<MaterialContext>() {
    @Override
    public Class<MaterialContext> getType() {
      return MaterialContext.class;
    }

    @Override
    public Key resolve(MaterialContext object) {
      return object.material().getKey();
    }
  });
    KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<DyeContext>() {

      @Override
      public Class<DyeContext> getType() {
        return DyeContext.class;
      }

      @Override
      public Key resolve(DyeContext object) {
        return NamespacedKey.minecraft(object.color().name().toLowerCase(Locale.ENGLISH));
      }
    });
    KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<EntityContext>() {

      @Override
      public Class<EntityContext> getType() {
        return EntityContext.class;
      }

      @Override
      public Key resolve(EntityContext object) {
        Entity entity = object.entity();
        return NamespacedKey.minecraft(entity.getName().toLowerCase(Locale.ENGLISH));
      }
    });
    KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<ItemContext>() {
      @Override
      public Class<ItemContext> getType() {
        return ItemContext.class;
      }

      @Override
      public Key resolve(ItemContext object) {
        return object.itemStack().getType().getKey();
      }
    });

  }
  static ActionType create(Key key) {
    ActionTypeImpl type = new ActionTypeImpl(key);
    RegistryContainer.registryContainer()
        .editRegistry(RegistryKeys.ACTION_TYPES, r -> r.register(type));
    return type;
  }

  @Override
  public @NotNull Key key() {
    return key;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ActionType type)) {
      return false;
    }
    return key.equals(type.key());
  }

}
