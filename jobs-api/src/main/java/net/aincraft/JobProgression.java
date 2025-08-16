package net.aincraft;

import java.math.BigDecimal;

public interface JobProgression extends JobProgressionView {

  JobProgression setExperience(BigDecimal experience);

  default JobProgression addExperience(BigDecimal experience) {
    return setExperience(experience.add(getExperience()));
  }
}
