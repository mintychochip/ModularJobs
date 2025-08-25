package net.aincraft.job.model;

import java.math.BigDecimal;

public record JobProgressionRecord(String playerId, String jobKey, BigDecimal experience) {

}
