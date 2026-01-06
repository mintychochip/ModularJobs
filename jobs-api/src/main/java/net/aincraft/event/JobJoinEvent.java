package net.aincraft.event;

import net.aincraft.Job;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player joins or rejoins a job.
 */
public class JobJoinEvent extends AbstractEvent {

    private final Player player;
    private final Job job;
    private final int level;
    private final boolean rejoin;

    public JobJoinEvent(@NotNull Player player, @NotNull Job job, int level, boolean rejoin) {
        this.player = player;
        this.job = job;
        this.level = level;
        this.rejoin = rejoin;
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
     * Gets the player's level (1 for new joins, restored level for rejoins).
     */
    public int getLevel() {
        return level;
    }

    /**
     * Whether this is a rejoin (player previously left this job).
     */
    public boolean isRejoin() {
        return rejoin;
    }
}
