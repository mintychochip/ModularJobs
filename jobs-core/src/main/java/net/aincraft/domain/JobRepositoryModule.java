package net.aincraft.domain;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.aincraft.domain.repository.JobRepository;

public class JobRepositoryModule extends AbstractModule {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public JobRepository jobRepository() {

  }
}
