package com.javaigua.stocks;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * A set of entities handled in this application.
 */
public class Domain {

  /**
   * A representation of the information and actions of a Stock.
   */
  public static class Stock {
    private final Integer id;
    private final String name;
    private final Double currentPrice;
    private final Instant lastUpdate;

    public Stock() {
      this.id = 0;
      this.name = "";
      this.currentPrice = 0d;
      this.lastUpdate = Instant.now();
    }

    public Stock(Integer id, String name, Double currentPrice, Instant lastUpdate) {
      this.id = id;
      this.name = name;
      this.currentPrice = currentPrice;
      this.lastUpdate = lastUpdate;
    }

    public Integer getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public Double getCurrentPrice() {
      return currentPrice;
    }

    public Instant getLastUpdate() {
      return lastUpdate;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      
      Stock other = (Stock) obj;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equals(other.id))
        return false;
      
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      
      return true;
    }

    @Override
    public String toString() {
      return new StringBuilder()
        .append("[id=").append(id)
        .append(", name=").append(name)
        .append(", currentPrice=").append(currentPrice)
        .append(", lastUpdate=").append(lastUpdate)
        .append("]")
        .toString();
    }
  }

  /**
   * A representation of a collection of stocks.
   */
  public static class Stocks{
    private final List<Stock> stocks;

    public Stocks() {
      this.stocks = new ArrayList<>();
    }

    public Stocks(List<Stock> stocks) {
      this.stocks = stocks;
    }

    public List<Stock> getStocks() {
      return stocks;
    }
  }

}
