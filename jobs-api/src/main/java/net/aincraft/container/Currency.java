package net.aincraft.container;

/**
 * Represents a currency for payable amounts.
 * Extensible for custom currencies via plugins.
 */
public interface Currency {

  Currency USD = of("USD", "$");
  Currency EUR = of("EUR", "€");
  Currency GBP = of("GBP", "£");
  Currency JPY = of("JPY", "¥");
  Currency CNY = of("CNY", "¥");

  /**
   * Creates a new currency with the given identifier and symbol.
   * Use this for custom currencies in plugins.
   */
  static Currency of(String identifier, String symbol) {
    return new CurrencyImpl(identifier, symbol);
  }

  String identifier();

  String symbol();

  /**
   * Default implementation for standard and custom currencies.
   */
  record CurrencyImpl(String identifier, String symbol) implements Currency {}
}

