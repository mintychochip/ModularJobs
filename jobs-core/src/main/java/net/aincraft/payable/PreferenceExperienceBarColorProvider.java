package net.aincraft.payable;

import com.google.inject.Inject;
import dev.mintychochip.mint.Mint;
import dev.mintychochip.mint.preferences.Preference;
import dev.mintychochip.mint.preferences.PreferenceService;
import net.kyori.adventure.bossbar.BossBar.Color;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * ExperienceBarColorProvider that reads the player's color preference from Mint's PreferenceService.
 * Falls back to a secondary provider if preferences are unavailable.
 */
@NullMarked
public final class PreferenceExperienceBarColorProvider implements ExperienceBarColorProvider {

    private final Preference<Color> preference;
    private final ExperienceBarColorProvider fallback;

    @Inject
    public PreferenceExperienceBarColorProvider(
            ExperienceBarColorPreference preference,
            @Fallback ExperienceBarColorProvider fallback) {
        this.preference = preference;
        this.fallback = fallback;
    }

    @Override
    public Color getColor(Player player) {
        if (!Mint.PREFERENCE_SERVICE.isLoaded()) {
            return fallback.getColor(player);
        }

        PreferenceService service = Mint.PREFERENCE_SERVICE.get();
        return service.get(player.getUniqueId(), preference);
    }
}
