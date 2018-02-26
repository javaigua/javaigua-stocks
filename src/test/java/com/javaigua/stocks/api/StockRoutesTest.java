package com.javaigua.stocks.api;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.model.*;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.StatusCodes;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.javaigua.stocks.actors.StockRegistryActor;

/**
 * A test suit for the RESTful endpoints of this application.
 */
public class StockRoutesTest extends JUnitRouteTest {

  private TestRoute appRoute;

  @Before
  public void initClass() {
    final Config config = ConfigFactory.load("reference");
    ActorSystem system = ActorSystem.create(config.getString("application.name"), config);
    ActorRef stockRegistryActor = system.actorOf(StockRegistryActor.props(), "stockRegistry");
    StocksServer server = new StocksServer(system, stockRegistryActor);
    appRoute = testRoute(server.createRoute());
  }

  @Test
  public void testHandleEmptyGET() {
    appRoute.run(HttpRequest.GET("/stocks"))
      .assertStatusCode(StatusCodes.OK)
      .assertMediaType("application/json")
      .assertEntity("{\"stocks\":[]}");
  }

  @Test
  public void testHandleNonEmptyGET() {
    appRoute.run(HttpRequest.POST("/stocks")
      .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
          "{\"id\": 1, \"name\": \"ABC\", \"currentPrice\": 2}"))
      .assertStatusCode(StatusCodes.CREATED);
    
    appRoute.run(HttpRequest.GET("/stocks"))
      .assertStatusCode(StatusCodes.OK)
      .assertMediaType("application/json");
  }

  @Test
  public void testHandlePOST() {
    appRoute.run(HttpRequest.POST("/stocks")
      .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
          "{\"id\": 1, \"name\": \"ABC\", \"currentPrice\": 2}"))
      .assertStatusCode(StatusCodes.CREATED)
      .assertMediaType("application/json")
      .assertEntity("{\"description\":\"Stock 1 created.\"}");
  }

  @Test
  public void testHandleGET() {
    appRoute.run(HttpRequest.POST("/stocks")
      .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
          "{\"id\": 1, \"name\": \"ABC\", \"currentPrice\": 2}"))
      .assertStatusCode(StatusCodes.CREATED);
    
    appRoute.run(HttpRequest.GET("/stocks/1"))
      .assertStatusCode(StatusCodes.OK)
      .assertMediaType("application/json");
  }

  @Test
  public void testHandlePUT() {
    appRoute.run(HttpRequest.POST("/stocks")
      .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
          "{\"id\": 1, \"name\": \"ABC\", \"currentPrice\": 2}"))
      .assertStatusCode(StatusCodes.CREATED);
    
    appRoute.run(HttpRequest.DELETE("/stocks/1"))
      .assertStatusCode(StatusCodes.OK)
      .assertMediaType("application/json")
      .assertEntity("{\"description\":\"Stock 1 deleted.\"}");
  }

  @Test
  public void testDELETE() {
    appRoute.run(HttpRequest.POST("/stocks")
      .withEntity(MediaTypes.APPLICATION_JSON.toContentType(),
          "{\"id\": 1, \"name\": \"ABC\", \"currentPrice\": 2}"))
      .assertStatusCode(StatusCodes.CREATED);
    
    appRoute.run(HttpRequest.DELETE("/stocks/1"))
      .assertStatusCode(StatusCodes.OK)
      .assertMediaType("application/json")
      .assertEntity("{\"description\":\"Stock 1 deleted.\"}");
  }
}
