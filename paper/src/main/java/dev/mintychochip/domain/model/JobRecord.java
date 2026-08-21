package dev.mintychochip.domain.model;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable definition of a job's configuration: identity, presentation, leveling,
 * payable curves, and upgrade/perk rules.
 *
 * @param jobKey         the unique job key
 * @param displayName    the human-readable job name
 * @param description    the job description
 * @param maxLevel       the maximum achievable level for the job
 * @param levellingCurve the expression describing experience-to-level progression
 * @param payableCurves  payable type key to curve expression mappings
 * @param upgradeLevel   the level at which the job may be upgraded
 * @param perkUnlocks    map of level to the perk keys unlocked at that level
 */
public record JobRecord(@NotNull String jobKey, @NotNull String displayName,
                        @NotNull String description, int maxLevel,
                        @NotNull String levellingCurve,
                        @NotNull Map<String, String> payableCurves,
                        int upgradeLevel,
                        @NotNull Map<Integer, List<String>> perkUnlocks) {

}
