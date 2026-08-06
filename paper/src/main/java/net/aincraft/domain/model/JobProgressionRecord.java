package net.aincraft.domain.model;

import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;

public record JobProgressionRecord(@NotNull String playerId, @NotNull JobRecord jobRecord,
                                   @NotNull BigDecimal experience) {

}
