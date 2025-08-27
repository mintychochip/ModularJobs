package net.aincraft.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import net.aincraft.commands.TopCommand.JobsTopPageProvider;

public final class CommandModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JobsTopPageProvider.class).in(Singleton.class);
    Multibinder<JobsCommand> binder = Multibinder.newSetBinder(binder(), JobsCommand.class);
    binder.addBinding().to(JoinCommand.class);
    binder.addBinding().to(TopCommand.class);
    binder.addBinding().to(InfoCommand.class);
    binder.addBinding().to(LeaveCommand.class);
  }
}
