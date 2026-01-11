package net.aincraft.event;

import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player's upgrade tree is reset.
 */
public class NodeResetEvent extends AbstractEvent {

    private final Player player;
    private final String jobKey;
    private final Set<String> revokedNodes;

    public NodeResetEvent(@NotNull Player player, @NotNull String jobKey, @NotNull Set<String> revokedNodes) {
        this.player = player;
        this.jobKey = jobKey;
        this.revokedNodes = Set.copyOf(revokedNodes);
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getJobKey() {
        return jobKey;
    }

    /**
     * Gets the set of node keys that were revoked.
     */
    @NotNull
    public Set<String> getRevokedNodes() {
        return revokedNodes;
    }
}
