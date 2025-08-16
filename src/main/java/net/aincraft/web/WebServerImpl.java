package net.aincraft.web;

import io.undertow.Undertow;

public class WebServerImpl implements WebServer{

  private Undertow server = null;

  public WebServerImpl(Undertow server) {
    this.server = server;
  }

  @Override
  public void start() {
  }

  @Override
  public void shutdown() {
    if (server != null) {
      server.stop();
    }
  }
}
