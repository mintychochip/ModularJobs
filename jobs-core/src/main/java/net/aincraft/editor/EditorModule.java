package net.aincraft.editor;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import net.aincraft.editor.json.GsonProvider;

/**
 * Guice module for the ModularJobs web editor subsystem.
 * <p>
 * Provides bindings for:
 * <ul>
 *   <li>{@link Gson} - configured JSON serializer with custom type adapters</li>
 *   <li>{@link BytebinClient} - HTTP client for bytebin paste service</li>
 *   <li>{@link EditorConfig} - editor configuration</li>
 *   <li>{@link EditorService} - service for export/import operations</li>
 *   <li>{@link EditorSessionStore} - session management</li>
 * </ul>
 */
public final class EditorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Gson.class).toProvider(GsonProvider.class).in(Singleton.class);
        bind(BytebinClient.class).to(BytebinClientImpl.class).in(Singleton.class);
        bind(EditorService.class).to(EditorServiceImpl.class).in(Singleton.class);
        bind(EditorSessionStore.class).in(Singleton.class);
        bind(EditorConfig.class).toInstance(EditorConfig.defaults());
    }
}
