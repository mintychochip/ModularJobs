package net.aincraft.domain.model;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public record JobRecord(@NotNull String jobKey, @NotNull String displayName,
                        @NotNull String description, int maxLevel,
                        @NotNull String levellingCurve,
                        @NotNull Map<String, String> payableCurves,
                        int upgradeLevel,
                        @NotNull Map<Integer, List<String>> perkUnlocks) {

}
