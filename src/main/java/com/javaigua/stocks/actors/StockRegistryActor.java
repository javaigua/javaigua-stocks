package com.javaigua.stocks.actors;

import java.util.Optional;
import java.util.ArrayList;

import scala.concurrent.Future;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.dispatch.Futures;
import akka.japi.pf.DeciderBuilder;
import static akka.pattern.Patterns.pipe;
import static akka.pattern.Patterns.ask;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.javaigua.stocks.Domain.Stock;
import com.javaigua.stocks.Domain.Stocks;

/**
 * A distributed registry for stocks handled in this application.
 *
 * Every stock entity is an actor supervised by this one, therefore
 * the processing of messages sent to this actor is distributed.
 * A reference lookup, by the given stock id, is performed upon arrival
 * of every message sent to this actor. The message is then forwarded to
 * this stock actor, except when the whole list of stocks is asked, 
 * in which case all children data is aggreagated and returned.
 */
public class StockRegistryActor extends AbstractActor {

  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
  final Config config = ConfigFactory.load();
  
  final ExecutionContext ec = getContext().dispatcher();

  // Supervision strategy for child actors
  private static SupervisorStrategy strategy =
    new OneForOneStrategy(10, Duration.create(1, "minute"),
      DeciderBuilder
        .matchAny(o -> SupervisorStrategy.restart())
        .build());

  /**
   * Convenient actor builder
   */
  public static Props props() {
    return Props.create(StockRegistryActor.class);
  }

  @Override
  public SupervisorStrategy supervisorStrategy() {
    return strategy;
  }

  /** 
   * Main entry point of messages handled by this actor
   */
  @Override
  public Receive createReceive(){
    return receiveBuilder()
      .match(StockRegistryMessages.GetStocks.class, 
        getStocks -> {
          pipe(aggregateStocks(), ec).to(getSender());
        }
      )
      .match(StockRegistryMessages.CreateStock.class, // handle CreateStock msgs
        createStock -> {
          Optional<ActorRef> stockActorRef = getStockActorRef(createStock.getStock().getId());
          if (stockActorRef.isPresent()) {
            getSender().tell(new StockRegistryMessages.ActionPerformed(
              String.format("Stock %s already exists.", createStock.getStock().getId())), getSelf());
          } else {
            getStockActorRefOrCreate(createStock.getStock().getId())
              .forward(createStock, getContext());
          }
        }
      )
      .match(StockRegistryMessages.UpdateStock.class, // handle UpdateStock msgs
        updateStock -> {
          Optional<ActorRef> stockActorRef = getStockActorRef(updateStock.getStock().getId());
          if (stockActorRef.isPresent()) {
            getStockActorRefOrCreate(updateStock.getStock().getId())
              .forward(updateStock, getContext());
          } else {
            getSender().tell(new StockRegistryMessages.ActionPerformed(
              String.format("Stock %s not found.", updateStock.getStock().getId())), getSelf());
          }
        }
      )
      .match(StockRegistryMessages.GetStock.class, // handle GetStock msgs
        getStock -> {
          Optional<ActorRef> stockActorRef = getStockActorRef(getStock.getId());
          if (stockActorRef.isPresent())
            stockActorRef.get().forward(getStock, getContext());
          else
            getSender().tell(Optional.ofNullable(null), getSelf());
        }
      )
      .match(StockRegistryMessages.DeleteStock.class, // handle DeleteStock msgs
        deleteStock -> {
          Optional<ActorRef> stockActorRef = getStockActorRef(deleteStock.getId());
          if (stockActorRef.isPresent())
            stockActorRef.get().forward(deleteStock, getContext());
          else
            getSender().tell(new StockRegistryMessages.ActionPerformed(
              String.format("Stock not found.", deleteStock.getId())), getSelf());
        }
      )
      .matchAny(unknown -> log.info("Unknown message received: {}", unknown))
      .build();
  }
  
  /**
   * An aggregator of data for the stocks in this registry.
   *
   * Every child is asked its info, which is then put in the domain class Stocks.
   */
  private Future<Stocks> aggregateStocks() {
    final int TIMEOUT = config.getInt("application.timeout-millis");
    StockRegistryMessages.GetStock getStock = new StockRegistryMessages.GetStock(0);
    final ArrayList<Future<Object>> futures = new ArrayList<>();
    
    // to aggregate information we ask every child of this actor for its data
    getContext().getChildren().forEach(child -> futures.add(ask(child, getStock, TIMEOUT)));
    
    // we collect the info in a Stocks domain object
    final Future<Stocks> stocksAggregate = Futures.sequence(futures, ec).map((iterable) -> {
      ArrayList<Stock> stockList = new ArrayList<>();
      for (Object o : iterable) {
        Optional<Stock> s = (Optional<Stock>) o;
        if (s.isPresent())
          stockList.add(s.get());
      }
      return new Stocks(stockList);
    }, ec);
    
    return stocksAggregate;
  }
  
  /**
   * Utility to perform Stock actor reference lookup by id.
   */
  private Optional<ActorRef> getStockActorRef(Integer id) {
    return getContext().findChild(id.toString());
  }

  /**
   * Utility to perform Stock actor reference lookup by id, creating a new
   * one in case it is alredy present in the actor system.
   */
  private ActorRef getStockActorRefOrCreate(Integer id) {
    return getStockActorRef(id).orElseGet(
      () -> getContext().actorOf(StockActor.props(), id.toString()));
  }
}
