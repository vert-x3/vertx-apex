package io.vertx.ext.web.handler.sse;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;

public class SSETestReceiveData extends SSETestBase {

	@Test
	public void testSimpleDataHandler() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
		final String message = "Happiness is a warm puppy";
		final EventSource eventSource = eventSource();
		eventSource.connect("/sse?token=" + TOKEN, handler -> {
				assertTrue(handler.succeeded());
				assertFalse(handler.failed());
				assertNull(handler.cause());
				assertNotNull(connection);
			eventSource.onMessage(msg -> {
        assertEquals(message + "\n", msg);
        latch.countDown();
			});
			connection.data(message);
		});
		awaitLatch(latch);
	}

	@Test
	public void testMultipleDataHandler() throws InterruptedException {
	  CountDownLatch latch = new CountDownLatch(1);
		final List<String> quotes = createData();
		final EventSource eventSource = eventSource();
		eventSource.connect("/sse?token=" + TOKEN, handler -> {
      assertTrue(handler.succeeded());
      assertFalse(handler.failed());
      assertNull(handler.cause());
      assertNotNull(connection);
			eventSource.onMessage(msg -> {
				final StringJoiner joiner = new StringJoiner("\n");
				quotes.forEach(joiner::add);
        assertEquals(joiner.toString() + "\n", msg);
        latch.countDown();
      });
      connection.data(quotes);
    });
    awaitLatch(latch);
	}

	@Test
	public void testConsecutiveDataHandler() throws InterruptedException {
	  CountDownLatch latch = new CountDownLatch(1);
		final List<String> quotes = createData();
		final EventSource eventSource = eventSource();
		eventSource.connect("/sse?token=" + TOKEN, handler -> {
      assertTrue(handler.succeeded());
      assertFalse(handler.failed());
      assertNull(handler.cause());
      assertNotNull(connection);
			final List<String> received = new ArrayList<>();
			eventSource.onMessage(msg -> {
				received.add(msg.substring(0, msg.length() - 1)); /* remove trailing linefeed */
				if (received.size() == quotes.size()) {
				  for (int i = 0; i < received.size(); i++) {
            assertEquals("Received quotes don't match", quotes.get(i), received.get(i));
          }
          latch.countDown();
				}
			});
			quotes.forEach(connection::data);
		});
		awaitLatch(latch);
	}

	@Test
	public void testEventHandler() throws InterruptedException {
	  CountDownLatch latch = new CountDownLatch(1);
		final String eventName = "quotes";
		final List<String> quotes = createData();
		final EventSource eventSource = eventSource();
		eventSource.connect("/sse?token=" + TOKEN, handler -> {
			assertTrue(handler.succeeded());
			assertFalse(handler.failed());
			assertNull(handler.cause());
			assertNotNull(connection);
			eventSource.onEvent("wrong", msg -> {
				throw new RuntimeException("this handler should not be called, at all !");
			});
			eventSource.onEvent(eventName, msg -> {
				final StringJoiner joiner = new StringJoiner("\n");
				quotes.forEach(joiner::add);
				assertEquals(joiner.toString() + "\n", msg);
				latch.countDown();
			});
			connection.event(eventName, quotes);
		});
		awaitLatch(latch);
	}

	@Test
	public void testId() throws InterruptedException {
	  CountDownLatch latch = new CountDownLatch(1);
		final String id = "SomeIdentifier";
		final List<String> quotes = createData();
		final EventSource eventSource = eventSource();
		eventSource.connect("/sse?token=" + TOKEN, handler -> {
      assertTrue(handler.succeeded());
      assertFalse(handler.failed());
      assertNull(handler.cause());
      assertNotNull(connection);
			eventSource.onMessage(msg -> {
				final StringJoiner joiner = new StringJoiner("\n");
				quotes.forEach(joiner::add);
        assertEquals(joiner.toString() + "\n", msg);
        assertEquals(id, eventSource.lastId());
				eventSource.close();
				eventSource.connect("/sse?token=" + TOKEN, eventSource.lastId(), secondHandler -> {
          assertTrue(handler.succeeded());
          assertFalse(handler.failed());
          assertNull(handler.cause());
          assertNotNull(connection);
          assertEquals(id, connection.lastId());
          latch.countDown();
				});
			});
			connection.id(id, quotes);
		});
		awaitLatch(latch);
	}

	private List<String> createData() {
		final List<String> data = new ArrayList<>(3);
		data.add("Happiness is a warm puppy");
		data.add("Bleh!");
		data.add("That's the secret of life... replace one worry with another");
		return data;
	}

}
