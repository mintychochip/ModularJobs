package net.aincraft.api.container;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.aincraft.api.Bridge;
import net.aincraft.config.YamlConfiguration;

public class ConfigurationValues {

  private static final Supplier<YamlConfiguration> YAML_CONFIGURATION_SUPPLIER = Suppliers.memoize(
      () -> YamlConfiguration.create(Bridge.bridge()
          .plugin(), "config.yml"));


  private static <T> Supplier<T> createSupplier(String key, Class<T> clazz) {
    return () -> YAML_CONFIGURATION_SUPPLIER.get().getObject(key, clazz);
  }

  public static final ConfigurationValue<Boolean> PAY_WHILE_RIDING = new ConfigurationValueImpl<>(
      createSupplier("pay-while-riding", Boolean.class));

  public static final ConfigurationValue<Boolean> USE_BLOCK_PROTECTION = new ConfigurationValueImpl<>(
      createSupplier("use-block-protection", Boolean.class));

  public static final ConfigurationValue<Boolean> PAY_FOR_EACH_CRAFT = new ConfigurationValueImpl<>(
      createSupplier("pay-for-each-craft", Boolean.class));

  public static final ConfigurationValue<Boolean> PAY_IN_CREATIVE = new ConfigurationValueImpl<>(
      createSupplier("pay-in-creative", Boolean.class));

  public static final ConfigurationValue<Long> COW_MILKING_PERIOD = new ConfigurationValueImpl<>(
      createSupplier("cow-milking-period", Long.class));

  public static final ConfigurationValue<Boolean> PAY_FOR_ITEM_DURABILITY_LOSS = new ConfigurationValueImpl<>(
      createSupplier("pay-for-item-durability-loss", Boolean.class));

  public static final ConfigurationValue<List<String>> DISABLED_WORLDS = new ConfigurationValueImpl<>(
      () -> YAML_CONFIGURATION_SUPPLIER.get().getStringList("disabled-worlds"));
}
