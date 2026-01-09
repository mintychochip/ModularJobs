package net.aincraft.payable;

import dev.mintychochip.mint.preferences.Preference;
import dev.mintychochip.mint.preferences.types.EnumPreferenceType;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

/**
 * Preference for customizing the experience bar color.
 */
@NullMarked
public final class ExperienceBarColorPreference implements Preference<Color> {

    private static final String NAME = "experience_bar_color";
    private static final String NAMESPACE = "modularjobs";
    private static final Component DISPLAY_NAME = Component.text("Modular Jobs");
    private static final Color DEFAULT = Color.BLUE;

    @Override
    public dev.mintychochip.mint.Plugin plugin() {
        return new dev.mintychochip.mint.Plugin(DISPLAY_NAME, NAMESPACE);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Component displayName() {
        return DISPLAY_NAME;
    }

    @Override
    public Color getDefault() {
        return DEFAULT;
    }

    @Override
    public EnumPreferenceType<Color> type() {
        return new EnumPreferenceType<>(Color.class);
    }
}
