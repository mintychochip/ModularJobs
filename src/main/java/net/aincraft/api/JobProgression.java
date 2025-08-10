package net.aincraft.api;

import com.google.common.base.Preconditions;

public interface JobProgression extends JobProgressionView {

  void setExperience(long experience);

  default void addExperience(long experience) {
    setExperience(experience + getExperience());
  }

  default void removeExperience(long experience) throws IllegalArgumentException {
    Preconditions.checkArgument(experience >= getExperience());
    setExperience(experience - getExperience());
  }
}
