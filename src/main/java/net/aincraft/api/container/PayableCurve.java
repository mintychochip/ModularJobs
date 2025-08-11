package net.aincraft.api.container;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.aincraft.api.JobProgression;
import net.kyori.adventure.key.Keyed;

public interface PayableCurve {

  BigDecimal apply(Map<String,Number> variables);

}
