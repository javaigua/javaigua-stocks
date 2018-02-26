package com.javaigua.stocks.actors;

import java.time.Instant;
import java.util.Optional;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.javaigua.stocks.Domain.Stock;

/**
 * An actor that controls a single Stock entity in this application.
 *
 * This actor is created and supervised by the registry. It performs
 * operations on a single Stock instance variable. Messages sent to this 
 * actor mutate or query this domain object, and since messages are 
 * processed sequentially one-by-one, there is no need to worry about 
 * concurrent modifications or other side effects.
 */
public class StockActor extends AbstractActor {

  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private Stock stock;

  /**
   * Convenient actor builder
   */
  public static Props props() {
    return Props.create(StockActor.class);
  }

  /** 
   * Main entry point of messages handled by this actor
   */
  @Override
  public Receive createReceive(){
    return receiveBuilder()
      .match(StockRegistryMessages.CreateStock.class, // handle CreateStock msgs
        createStock -> {
          this.stock = createStock.getStock();
          getSender().tell(new StockRegistryMessages.ActionPerformed(
            String.format("Stock %s created.", createStock.getStock().getId())), getSelf());
        }
      )
      .match(StockRegistryMessages.UpdateStock.class,  // handle UpdateStock msgs
        updateStock -> {
          if (this.stock != null) {
            this.stock = new Stock(
              this.stock.getId(),
              this.stock.getName(),
              updateStock.getStock().getCurrentPrice(),
              Instant.now());
            getSender().tell(new StockRegistryMessages.ActionPerformed(
              String.format("Stock %s updated.", updateStock.getStock().getId())), getSelf());
          } else {
            getSender().tell(new StockRegistryMessages.ActionPerformed(
              String.format("Nothing to update.", updateStock.getStock().getId())), getSelf());
            getContext().stop(getSelf());
          }
        }
      )
      .match(StockRegistryMessages.GetStock.class, // handle GetStock msgs
        getStock -> {
          getSender().tell(Optional.ofNullable(this.stock), getSelf());
        }
      )
      .match(StockRegistryMessages.DeleteStock.class, // handle DeleteStock msgs
        deleteStock -> {
          getSender().tell(
            new StockRegistryMessages.ActionPerformed(
              String.format("Stock %s deleted.", deleteStock.getId())), getSelf());
          getContext().stop(getSelf()); // kill this actor
        }
      )
      .matchAny(unknown -> log.info("Unknown message received: {}", unknown))
      .build();
  }
}
