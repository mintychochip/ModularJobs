package net.aincraft.job.model;

import java.math.BigDecimal;
import org.jetbrains.annotations.Nullable;

public record PayableRecord(String payableTypeKey, BigDecimal amount, @Nullable String currency) {

}
