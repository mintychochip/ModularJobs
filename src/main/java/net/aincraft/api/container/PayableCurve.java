package net.aincraft.api.container;

import java.math.BigDecimal;
import java.util.Map;

public interface PayableCurve {

  BigDecimal apply(Map<String,Number> variables);

}
