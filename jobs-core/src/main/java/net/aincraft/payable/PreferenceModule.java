package net.aincraft.payable;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.mintychochip.mint.Mint;
import dev.mintychochip.mint.preferences.PreferenceService;
import dev.mintychochip.mint.preferences.types.EnumPreferenceType;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.jspecify.annotations.NullMarked;

/**
 * Guice module for the preference-based experience bar color integration.
 * Only activates when Mint's PreferenceService is available.
 */
@NullMarked
public final class PreferenceModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bind the primary provider (preference-based)
        bind(ExperienceBarColorProvider.class)
                .to(PreferenceExperienceBarColorProvider.class);

        // Bind the fallback provider (theme-based)
        bind(ExperienceBarColorProvider.class)
                .annotatedWith(Fallback.class)
                .to(ThemeExperienceBarColorProvider.class);

        // Register preferences with Mint (eagerly, so it runs at startup)
        bind(PreferenceRegistrar.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    PreferenceService preferenceService() {
        return Mint.PREFERENCE_SERVICE.get();
    }

    @Provides
    @Singleton
    ExperienceBarColorPreference experienceBarColorPreference() {
        return new ExperienceBarColorPreference();
    }

    /**
     * Eager singleton that registers the preference with Mint's service.
     * Constructor runs automatically when the injector is created.
     */
    @Singleton
    static final class PreferenceRegistrar {
        @Inject
        PreferenceRegistrar(
                PreferenceService service,
                ExperienceBarColorPreference preference) {
            if (Mint.PREFERENCE_SERVICE.isLoaded()) {
                service.registerType(new EnumPreferenceType<>(Color.class));
                service.registerDisplayable(preference);
            }
        }
    }
}
