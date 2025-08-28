package net.aincraft.commands.top;

public class AbstractJobsTopPageConsumerImpl {

  private int calculateRank(int index, int pageNumber, int pageSize) {
    return (index + 1) + (pageNumber - 1) * pageSize;
  }
}
