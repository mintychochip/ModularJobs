package net.aincraft.payable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import net.aincraft.container.Currency;
import net.aincraft.container.EconomyProvider;
import org.jetbrains.annotations.NotNull;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarController;
import net.aincraft.container.ExperiencePayableHandler.ExperienceBarFormatter;
import net.aincraft.container.PayableAmount;
import net.aincraft.container.PayableHandler;
import net.aincraft.container.PayableType;
import net.aincraft.gui.craftux.CraftuxSurfaces;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Manual composition for payable types and experience bar (replaces Guice PayableModule).
 */
public final class PayableWiring {

  private static final String ECONOMY_TYPE = "modularjobs:economy";
  private static final String EXPERIENCE_TYPE = "modularjobs:experience";

  public final @NotNull EconomyProvider economyProvider;

  private PayableWiring(@NotNull EconomyProvider economyProvider) {
    this.economyProvider = economyProvider;
  }

  /**
   * Composes the experience and economy payable handlers, registers the corresponding
   * {@link PayableType}s in {@code payableTypeRegistry}, and resolves the economy provider via
   * {@link EconomyProviderFactory#createOrFail}. Returns the wiring exposing the chosen provider.
   */
  public static PayableWiring create(
      Plugin plugin,
      JobService jobService,
      Registry<PayableType> payableTypeRegistry,
      CraftuxSurfaces surfaces) {
    ExperienceBarColorProvider colorProvider = new ExperienceBarColorProvider();
    ExperienceBarController controller = new ExperienceBarControllerImpl(plugin, surfaces);
    ExperienceBarFormatter formatter = new ExperienceBarFormatterImpl(colorProvider);
    PayableHandler experienceHandler =
        new BufferedExperienceHandlerImpl(controller, formatter, jobService);

    EconomyProvider economyProvider = EconomyProviderFactory.createOrFail(plugin);
    PayableHandler economyHandler = economyHandlerFor(economyProvider);

    payableTypeRegistry.register(economyType(economyHandler));
    payableTypeRegistry.register(experienceType(experienceHandler));

    return new PayableWiring(economyProvider);
  }

  /** Delegates economy payables to the selected provider, including the blackhole fallback. */
  static PayableHandler economyHandlerFor(@NotNull EconomyProvider economyProvider) {
    return context -> economyProvider.deposit(
        context.playerId(), context.payable().amount());
  }

  private static PayableType economyType(PayableHandler handler) {
    Key key = NamespacedKey.fromString(ECONOMY_TYPE);
    return new PayableType() {
      private static final String FORMAT = "<#7ed278><symbol><amount></#7ed278>";

      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return key;
      }

      @Override
      public Component render(PayableAmount amount, int places) {
        String symbol = amount.currency().orElse(Currency.USD).symbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(places);
        nf.setMaximumFractionDigits(places);
        BigDecimal value = amount.value().setScale(places, RoundingMode.HALF_UP);
        return MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder()
            .tag("symbol", Tag.inserting(Component.text(symbol)))
            .tag("amount", Tag.inserting(Component.text(nf.format(value))))
            .build());
      }
    };
  }

  private static PayableType experienceType(PayableHandler handler) {
    Key key = NamespacedKey.fromString(EXPERIENCE_TYPE);
    return new PayableType() {
      private static final String FORMAT = "<#dac65c><amount>xp</#dac65c>";

      @Override
      public PayableHandler handler() {
        return handler;
      }

      @Override
      public Key key() {
        return key;
      }

      @Override
      public Component render(PayableAmount amount, int places) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(places);
        nf.setMaximumFractionDigits(places);
        BigDecimal value = amount.value().setScale(places, RoundingMode.HALF_UP);
        return MiniMessage.miniMessage().deserialize(FORMAT, TagResolver.builder()
            .tag("amount", Tag.inserting(Component.text(nf.format(value))))
            .build());
      }
    };
  }
}
