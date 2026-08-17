package net.aincraft.common.editor;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Payable (reward) data for editor operations.
 * Represents a single reward with type and amount.
 *
 * @param type   namespaced key for payable type (e.g. {@code modularjobs:experience})
 * @param amount BigDecimal value as string for precision
 */
public record PayableData(
    @SerializedName("type")
    @NotNull String type,

    @SerializedName("amount")
    @NotNull String amount
) {

  /**
   * Creates payable data for export.
   *
   * @param type namespaced key for payable type (e.g. "modularjobs:experience")
   * @param amount BigDecimal value as string for precision
   * @return new payable data instance
   */
  public static PayableData create(@NotNull String type, @NotNull String amount) {
    return new PayableData(type, amount);
  }
}
