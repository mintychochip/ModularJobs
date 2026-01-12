package net.aincraft.event;

import net.aincraft.upgrade.UpgradeNode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired after a player upgrades an upgrade node to a higher level.
 */
public class NodeUpgradeEvent extends AbstractEvent {

    private final Player player;
    private final String jobKey;
    private final UpgradeNode node;
    private final int previousLevel;
    private final int newLevel;

    public NodeUpgradeEvent(
        @NotNull Player player,
        @NotNull String jobKey,
        @NotNull UpgradeNode node,
        int previousLevel,
        int newLevel
    ) {
        this.player = player;
        this.jobKey = jobKey;
        this.node = node;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getJobKey() {
        return jobKey;
    }

    @NotNull
    public UpgradeNode getNode() {
        return node;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}
