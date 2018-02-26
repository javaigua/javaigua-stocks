package com.javaigua.stocks.actors;

import java.util.Optional;
import java.time.Instant;

import org.scalatest.junit.JUnitSuite;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.AbstractActor;
import scala.concurrent.duration.Duration;

import com.javaigua.stocks.Domain.Stock;
import com.javaigua.stocks.Domain.Stocks;

/**
 * A test suit for the StockActor class.
 */
public class StockActorTest extends JUnitSuite {
  
  static ActorSystem system;
  
  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }
  
  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testHandleCreateStock() {
    new TestKit(system) {{
      final Props props = Props.create(StockActor.class);
      final ActorRef subject = system.actorOf(props);
      final TestKit probe = new TestKit(system);
      
      within(duration("2 seconds"), () -> {
        subject.tell(
          new StockRegistryMessages.CreateStock(
            new Stock(1, "ABC", 2d, Instant.now())), 
          getRef());
        
        expectMsgPF(duration("2 seconds"), "Should create stock", (msg) -> {
          StockRegistryMessages.ActionPerformed action = (StockRegistryMessages.ActionPerformed) msg;
          Assert.assertEquals(action.getDescription(), "Stock 1 created.");
          return null;
        });

        expectNoMsg();
        return null;
      });
    }};
  }

  @Test
  public void testHandleGetStock() {
    new TestKit(system) {{
      final Props props = Props.create(StockActor.class);
      final ActorRef subject = system.actorOf(props);
      final TestKit probe = new TestKit(system);
      
      final Stock stock = new Stock(1, "ABC", 2d, Instant.now());
      within(duration("2 seconds"), () -> {
        subject.tell(
          new StockRegistryMessages.CreateStock(stock), 
          getRef());
        expectMsgClass(StockRegistryMessages.ActionPerformed.class);

        subject.tell(new StockRegistryMessages.GetStock(1), getRef());
        expectMsgPF(duration("1 seconds"), "Should retrieve stock", (msg) -> {
          Optional<Stock> s = (Optional<Stock>) msg;
          Assert.assertEquals(s.get(), stock);
          return null;
        });

        expectNoMsg();
        return null;
      });
    }};
  }

  @Test
  public void testHandleUpdateStock() {
    new TestKit(system) {{
      final Props props = Props.create(StockActor.class);
      final ActorRef subject = system.actorOf(props);
      final TestKit probe = new TestKit(system);
      
      final Stock stock = new Stock(1, "ABC", 2d, Instant.now());
      within(duration("2 seconds"), () -> {
        subject.tell(
          new StockRegistryMessages.CreateStock(stock), 
          getRef());
        expectMsgClass(StockRegistryMessages.ActionPerformed.class);

        final Stock updatedStock = new Stock(1, "CBA", 3d, Instant.now());
        subject.tell(new StockRegistryMessages.UpdateStock(updatedStock), getRef());
        expectMsgPF(duration("1 seconds"), "Should update stock", (msg) -> {
          StockRegistryMessages.ActionPerformed action = (StockRegistryMessages.ActionPerformed) msg;
          Assert.assertEquals(action.getDescription(), "Stock 1 updated.");
          return null;
        });

        expectNoMsg();
        return null;
      });
    }};
  }

  @Test
  public void testHandleDeleteStock() {
    new TestKit(system) {{
      final Props props = Props.create(StockActor.class);
      final ActorRef subject = system.actorOf(props);
      final TestKit probe = new TestKit(system);
      
      within(duration("2 seconds"), () -> {
        subject.tell(
          new StockRegistryMessages.CreateStock(
            new Stock(1, "ABC", 2d, Instant.now())), 
          getRef());
        expectMsgClass(StockRegistryMessages.ActionPerformed.class);

        subject.tell(new StockRegistryMessages.DeleteStock(1), getRef());
        expectMsgPF(duration("1 seconds"), "Should delete stock", (msg) -> {
          StockRegistryMessages.ActionPerformed action = (StockRegistryMessages.ActionPerformed) msg;
          Assert.assertEquals(action.getDescription(), "Stock 1 deleted.");
          return null;
        });

        expectNoMsg();
        return null;
      });
    }};
  }
}
