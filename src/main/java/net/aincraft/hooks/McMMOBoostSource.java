package net.aincraft.hooks;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.Provider;

public interface McMMOBoostSource extends BoostSource {

  void setBoostAmountProvider(Provider<SuperAbilityType,Double> boostAmountProvider);

}
