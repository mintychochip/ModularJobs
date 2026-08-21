package dev.mintychochip.container;

import java.math.BigDecimal;
import java.util.UUID;
import dev.mintychochip.JobProgressionView;
import net.kyori.adventure.bossbar.BossBar;
import org.jetbrains.annotations.NotNull;

/**
 * Payable handler for experience-based rewards.
 *
 * <p>Extends {@link PayableHandler} with hooks for customising the boss bar
 * used to present the experience reward and for controlling how that bar is
 * displayed to the player.</p>
 */
public interface ExperiencePayableHandler extends PayableHandler {

  /**
   * Formats a boss bar instance for a given experience reward context.
   */
  interface ExperienceBarFormatter {

    /**
     * Applies the reward details to the given boss bar.
     *
     * @param bossBar the boss bar to format and return
     * @param context details of the experience reward being presented
     * @return the formatted boss bar, which may be the supplied instance
     */
    BossBar format(@NotNull BossBar bossBar,
        @NotNull ExperienceBarContext context);

    /**
     * Sets the overlay style used when rendering the formatted boss bar.
     *
     * @param overlay the overlay to apply
     */
    void setOverlay(@NotNull BossBar.Overlay overlay);
  }

  /**
   * Controls when and how an experience reward's boss bar is displayed.
   */
  interface ExperienceBarController {

    /**
     * Displays the experience reward using the given formatter.
     *
     * @param context details of the experience reward to display
     * @param formatter formatter used to build the boss bar
     */
    void display(ExperienceBarContext context, ExperienceBarFormatter formatter);
  }

  /**
   * Immutable context describing an experience reward presentation.
   *
   * @param progression progression view associated with the player
   * @param playerId unique identifier of the player
   * @param amount experience amount being rewarded
   */
  record ExperienceBarContext(JobProgressionView progression, UUID playerId, BigDecimal amount) {}
}
