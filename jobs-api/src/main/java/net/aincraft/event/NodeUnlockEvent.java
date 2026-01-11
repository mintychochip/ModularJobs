package net.aincraft.event;

import net.aincraft.upgrade.UpgradeNode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired after a player unlocks an upgrade node.
 */
public class NodeUnlockEvent extends AbstractEvent {

    private final Player player;
    private final String jobKey;
    private final UpgradeNode node;

    public NodeUnlockEvent(@NotNull Player player, @NotNull String jobKey, @NotNull UpgradeNode node) {
        this.player = player;
        this.jobKey = jobKey;
        this.node = node;
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
}
