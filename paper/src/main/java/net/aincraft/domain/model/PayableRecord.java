package net.aincraft.domain.model;

import java.math.BigDecimal;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable reward record: a payable type, its amount, and an optional currency.
 *
 * @param payableTypeKey     the type key identifying how the reward is paid
 * @param amount             the reward amount
 * @param currencyIdentifier the currency holding the amount, or {@code null} when not
 *                           currency-backed
 */
public record PayableRecord(String payableTypeKey, BigDecimal amount, @Nullable String currencyIdentifier) {

}
