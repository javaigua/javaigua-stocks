package com.javaigua.stocks.actors;

import java.io.Serializable;

import com.javaigua.stocks.Domain.Stock;

/**
 * A set of messages, simple inmutable plain java objects, accepted and produced by 
 * the Stock and StockRegistry actors of the actor system.
 */
public interface StockRegistryMessages {

  /**
   * A message to signal the retrieval of all stock data.
   */
  class GetStocks implements Serializable {}

  /**
   * A message to signal the creation of a stock.
   */
  class CreateStock implements Serializable {
    private final Stock stock;

    public CreateStock(Stock stock) {
      this.stock = stock;
    }

    public Stock getStock() {
      return stock;
    }
  }

  /**
   * A message to signal the modification of a stock.
   */
  class UpdateStock implements Serializable {
    private final Stock stock;

    public UpdateStock(Stock stock) {
      this.stock = stock;
    }

    public Stock getStock() {
      return stock;
    }
  }

  /**
   * A message to signal the retrieval of a single stock.
   */
  class GetStock implements Serializable {
    private final Integer id;

    public GetStock(Integer id) {
      this.id = id;
    }

    public Integer getId() {
      return id;
    }
  }

  /**
   * A message to signal the elimination of a stock.
   */
  class DeleteStock implements Serializable {
    private final Integer id;

    public DeleteStock(Integer id) {
      this.id = id;
    }

    public Integer getId() {
      return id;
    }
  }

  /**
   * A message to convey the result of an action performed.
   */
  class ActionPerformed implements Serializable {
    private final String description;

    public ActionPerformed(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}