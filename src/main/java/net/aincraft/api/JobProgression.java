package net.aincraft.api;

import com.google.common.base.Preconditions;

public interface JobProgression extends JobProgressionView {

  void setExperience(double experience);

  default void addExperience(double experience) {
    setExperience(experience + getExperience());
  }
}
