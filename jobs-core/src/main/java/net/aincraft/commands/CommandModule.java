package net.aincraft.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import net.aincraft.commands.TopCommand.JobTopPageProviderImpl;
import org.bukkit.event.Listener;

public final class CommandModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JobTopPageProvider.class).to(JobTopPageProviderImpl.class).in(Singleton.class);

    Multibinder<Listener> listenerBinder = Multibinder.newSetBinder(binder(), Listener.class);
    listenerBinder.addBinding().to(DialogNavigationListener.class);

    Multibinder<JobsCommand> binder = Multibinder.newSetBinder(binder(), JobsCommand.class);
    binder.addBinding().to(JoinCommand.class);
    binder.addBinding().to(TopCommand.class);
    binder.addBinding().to(InfoCommand.class);
    binder.addBinding().to(LeaveCommand.class);
    binder.addBinding().to(LeaveAllCommand.class);
    binder.addBinding().to(ApplyEditsCommand.class);
    binder.addBinding().to(EditorCommand.class);
    binder.addBinding().to(StatsCommand.class);
    binder.addBinding().to(ArchiveCommand.class);
    // Unified boost command (replaces BoostsCommand, ItemBoostCommand, SourceCommand)
    binder.addBinding().to(BoostCommand.class);
    binder.addBinding().to(UpgradesCommand.class);
    // binder.addBinding().to(UpgradeCommand.class); // Disabled - requires JobPets dependency
    binder.addBinding().to(SetLevelCommand.class);
    binder.addBinding().to(AddLevelCommand.class);
    binder.addBinding().to(SubtractLevelCommand.class);
  }
}
