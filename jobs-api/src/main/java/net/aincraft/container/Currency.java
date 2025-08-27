package net.aincraft.container;

public interface Currency {

  Currency USD = new Currency() {
    @Override
    public String identifier() {
      return "USD";
    }

    @Override
    public String symbol() {
      return "$";
    }
  };

  String identifier();

  String symbol();
}

