package net.aincraft.container;

import java.util.UUID;
import net.aincraft.JobProgressionView;

public record BoostContext(
    ActionType type,
    JobProgressionView progression,
    UUID playerId,
    String worldName,
    Payable payable) {
}
