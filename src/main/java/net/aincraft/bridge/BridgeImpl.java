package net.aincraft.bridge;

import com.google.common.base.Preconditions;
import java.lang.reflect.Proxy;
import net.aincraft.api.Bridge;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.config.ConfigurationFactory;
import net.aincraft.config.YamlConfiguration;

public final class BridgeImpl implements Bridge {

  private final RegistryContainer registryContainer = new RegistryContainerImpl();
  @Override
  public ConfigurationFactory configurationFactory() {
    return (plugin, path) -> {
      String[] split = path.split("\\.");
      Preconditions.checkArgument(split.length >= 2);
      Preconditions.checkArgument(split[1].equals("yml") || split[1].equals("yaml"));
      YamlFileBackedConfigurationImpl impl = new YamlFileBackedConfigurationImpl(path, plugin);
      org.bukkit.configuration.file.YamlConfiguration config = impl.getConfig();
      return (YamlConfiguration) Proxy.newProxyInstance(YamlConfiguration.class.getClassLoader(),
          new Class[]{
              YamlConfiguration.class}, (proxy, method, args) -> {
            if ("getPlugin".equals(method.getName())) {
              return impl.getPlugin();
            }
            if ("reload".equals(method.getName())) {
              impl.reload();
              return null;
            }
            if ("save".equals(method.getName())) {
              impl.save();
              return null;
            }
            return method.invoke(config, args);
          });
    };
  }

  @Override
  public RegistryContainer registryContainer() {
    return registryContainer;
  }

}
