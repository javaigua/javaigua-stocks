package com.javaigua.stocks.api;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import akka.util.Timeout;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.PatternsCS;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.javaigua.stocks.Domain.Stock;
import com.javaigua.stocks.Domain.Stocks;
import com.javaigua.stocks.actors.StockRegistryMessages.GetStock;
import com.javaigua.stocks.actors.StockRegistryMessages.GetStocks;
import com.javaigua.stocks.actors.StockRegistryMessages.DeleteStock;
import com.javaigua.stocks.actors.StockRegistryMessages.CreateStock;
import com.javaigua.stocks.actors.StockRegistryMessages.UpdateStock;
import com.javaigua.stocks.actors.StockRegistryMessages.ActionPerformed;

/**
 * Stocks RESTful API routes mapping.
 */
public class StockRoutes extends AllDirectives {

  final private LoggingAdapter log;  
  final private ActorRef stockRegistryActor;
  final Config config = ConfigFactory.load();
  Timeout timeout = new Timeout(
    Duration.create(config.getInt("application.timeout-millis"), TimeUnit.MILLISECONDS));

  public StockRoutes(ActorSystem system, ActorRef stockRegistryActor) {
    this.stockRegistryActor = stockRegistryActor;
    log = Logging.getLogger(system, this);
  }

  /**
   * Creates routes
   */
  public Route routes() {
    return route(pathPrefix("stocks", () ->
      route(
        getAllOrPostStocks(),
        path(PathMatchers.segment(), id -> route(
          getStock(id),
          updateStock(id),
          deleteStock(id)
        ))
      )
    ));
  }

  /**
   * Mapping to handle GET requests.
   */
  private Route getStock(String id) {
    return get(() -> {
      if (!parceId(id).isPresent()) return complete(StatusCodes.BAD_REQUEST);
      
      CompletionStage<Optional<Stock>> optionalStock = PatternsCS
        .ask(stockRegistryActor, new GetStock(Integer.valueOf(id)), timeout)
        .thenApply(obj -> (Optional<Stock>) obj);

      return rejectEmptyResponse(() ->
        onSuccess(
          () -> optionalStock,
          performed -> {
            if (performed.isPresent())
              return complete(StatusCodes.OK, (Stock) performed.get(), Jackson.<Stock>marshaller());
            else
              return complete(StatusCodes.NOT_FOUND);
          }
        )
      );
    });
  }

  /**
   * Mapping to handle DELETE requests.
   */
  private Route deleteStock(String id) {
    return delete(() -> {
      if (!parceId(id).isPresent()) return complete(StatusCodes.BAD_REQUEST);
      
      CompletionStage<ActionPerformed> stockDeleted = PatternsCS
        .ask(stockRegistryActor, new DeleteStock(Integer.valueOf(id)), timeout)
        .thenApply(obj -> (ActionPerformed) obj);

        return onSuccess(() -> stockDeleted,
          performed -> {
            log.info("Deleted stock [{}]: {}", id, performed.getDescription());
            return complete(StatusCodes.OK, performed, Jackson.marshaller());
          }
        );
      });
  }

  /**
   * Mapping to handle PUT requests.
   */
  private Route updateStock(String id) {
    return pathEnd(() -> {
      if (!parceId(id).isPresent()) return complete(StatusCodes.BAD_REQUEST);
      
      return route(
        put(() ->
          entity(
            Jackson.unmarshaller(Stock.class),
              stock -> {
                Stock stockWithPathId = new Stock(
                  Integer.valueOf(id),
                  stock.getName(),
                  stock.getCurrentPrice(),
                  null);
                CompletionStage<ActionPerformed> stockUpdated = PatternsCS
                  .ask(stockRegistryActor, new UpdateStock(stockWithPathId), timeout)
                  .thenApply(obj -> (ActionPerformed) obj);
                return onSuccess(() -> stockUpdated,
                  performed -> {
                    log.info("Updated stock {}: {}", stockWithPathId.getId(), performed.getDescription());
                    return complete(StatusCodes.OK, performed, Jackson.marshaller());
                  }
                );
              }
          )
        )
      );
    });
  }

  /**
   * Mapping to handle GET and POST requests with JSON formated payloads.
   */
  private Route getAllOrPostStocks() {
    return pathEnd(() ->
      route(
        get(() -> {
          CompletionStage<Stocks> futureStocks = PatternsCS
            .ask(stockRegistryActor, new GetStocks(), timeout)
            .thenApply(obj -> (Stocks) obj);
          return onSuccess(() -> futureStocks,
            stocks -> complete(StatusCodes.OK, stocks, Jackson.marshaller()));
        }),
        post(() ->
          entity(
            Jackson.unmarshaller(Stock.class),
              stock -> {
                CompletionStage<ActionPerformed> stockCreated = PatternsCS
                  .ask(stockRegistryActor, new CreateStock(stock), timeout)
                  .thenApply(obj -> (ActionPerformed) obj);
                return onSuccess(() -> stockCreated,
                  performed -> {
                    log.info("Created stock {}: {}", stock.toString(), performed.getDescription());
                    return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                  }
                );
              }
          )
        )
      )
    );
  }
  
  private Optional<Integer> parceId(String id) {
    try {
      return Optional.of(Integer.valueOf(id));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
  
}
