package net.aincraft.api;

public interface JobProgression extends JobProgressionView {

  void setExperience(double experience);

  default void addExperience(double experience) {
    setExperience(experience + getExperience());
  }
}
