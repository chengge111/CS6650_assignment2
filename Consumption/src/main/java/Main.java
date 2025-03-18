import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.*;

public class Main {
  private static final String QUEUE_NAME = "skiersQueue";
  private static final String RABBITMQ_HOST = "172.31.25.112";
  private static final String USERNAME = "admin";
  private static final String PASSWORD = "admin123";
  private static final int THREAD_COUNT = 200;
  private static final ConcurrentHashMap<Integer, List<JsonObject>> skierData = new ConcurrentHashMap<>();
  private static final Gson gson = new Gson();

  public static void main(String[] args) {
    try {
      // connect RabbitMQ
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(RABBITMQ_HOST);
      factory.setUsername(USERNAME);
      factory.setPassword(PASSWORD);
      Connection connection = factory.newConnection();

      //
      ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
      for (int i = 0; i < THREAD_COUNT; i++) {
        executorService.submit(new ConsumerThread(connection));
      }

      //
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          System.out.println("Shutting down Consumer...");
          executorService.shutdown();
          connection.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }));

    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  static class ConsumerThread implements Runnable {
    private final Connection connection;
    private Channel channel;

    public ConsumerThread(Connection connection) {
      this.connection = connection;
      this.channel = createChannel();
    }

    @Override
    public void run() {
      try {
        System.out.println(Thread.currentThread().getName() + " [*] Waiting for messages from RabbitMQ...");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
          System.out.println(Thread.currentThread().getName() + " [x] Received: " + message);
          processMessage(message);

          // ACK manually
          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
      } catch (IOException e) {
        System.out.println("Consumer encountered an error. Restarting...");
        reconnect();
      }
    }

    private Channel createChannel() {
      try {
        Channel ch = connection.createChannel();
        ch.queueDeclare(QUEUE_NAME, true, false, false, null);
        return ch;
      } catch (IOException e) {
        System.out.println("Failed to create RabbitMQ channel. Retrying...");
        reconnect();
        return null;
      }
    }

    private void reconnect() {
      while (true) {
        try {
          Thread.sleep(5000);
          this.channel = createChannel();
          if (this.channel != null) {
            System.out.println("Reconnected to RabbitMQ.");
            break;
          }
        } catch (InterruptedException ignored) {}
      }
    }
  }

  // JSON，put it into ConcurrentHashMap
  private static void processMessage(String message) {
    try {
      JsonObject json = gson.fromJson(message, JsonObject.class);
      int skierID = json.get("skierID").getAsInt();
      String liftData = json.get("liftID").getAsString() + "," + json.get("time").getAsString();
      if (!skierData.containsKey(skierID)) {
        skierData.put(skierID, Collections.synchronizedList(new ArrayList<>()));
      }
      skierData.get(skierID).add(json);
      System.out.println("Stored skierID " + skierID + " -> " + liftData);
    } catch (Exception e) {
      System.out.println("Failed to parse message: " + message);
    }
  }
}
