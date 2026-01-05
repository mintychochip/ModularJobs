package net.aincraft.event;

import net.aincraft.Job;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player leaves a job.
 */
public class JobLeaveEvent extends AbstractEvent {

    private final Player player;
    private final Job job;
    private final int finalLevel;

    public JobLeaveEvent(@NotNull Player player, @NotNull Job job, int finalLevel) {
        this.player = player;
        this.job = job;
        this.finalLevel = finalLevel;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Job getJob() {
        return job;
    }

    /**
     * Gets the player's level at time of leaving.
     */
    public int getFinalLevel() {
        return finalLevel;
    }
}
