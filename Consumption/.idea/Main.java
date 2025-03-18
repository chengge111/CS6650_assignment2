import com.rabbitmq.client.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

public class Main {
  private static final String QUEUE_NAME = "skiersQueue";
  private static final String RABBITMQ_HOST = "18.236.78.132";
  private static final String USERNAME = "admin";
  private static final String PASSWORD = "admin123";
  private static final int THREAD_COUNT = 300;

  private static final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> skierRidesMap =
      new ConcurrentHashMap<>();

  private static final AtomicLong processedMessageCount = new AtomicLong(0);
  private static long startTime = System.currentTimeMillis();

  public static void main(String[] args) {
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down consumer...");
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
      }
      printStats();
    }));

    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(RABBITMQ_HOST);
      factory.setUsername(USERNAME);
      factory.setPassword(PASSWORD);
      factory.setAutomaticRecoveryEnabled(true);

      Connection connection = factory.newConnection();

      for (int i = 0; i < THREAD_COUNT; i++) {
        executorService.submit(new ConsumerTask(connection, skierRidesMap, processedMessageCount));
      }

      executorService.submit(() -> {
        while (!Thread.currentThread().isInterrupted()) {
          try {
            Thread.sleep(10000);
            printStats();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      });

    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  private static void printStats() {
    long currentTime = System.currentTimeMillis();
    long elapsedTime = (currentTime - startTime) / 1000;
    long messageCount = processedMessageCount.get();

    System.out.println("==== Performance Statistics ====");
    System.out.println("Total messages processed: " + messageCount);
    System.out.println("Running time: " + elapsedTime + " seconds");
    System.out.println("Processing rate: " + (elapsedTime > 0 ? messageCount / elapsedTime : 0) + " messages/second");
    System.out.println("Unique skiers recorded: " + skierRidesMap.size());
    System.out.println("================================");
  }
}

class ConsumerTask implements Runnable {
  private final Connection connection;
  private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> skierRidesMap;
  private final AtomicLong processedMessageCount;
  private final Gson gson = new Gson();

  public ConsumerTask(Connection connection,
      ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> skierRidesMap,
      AtomicLong processedMessageCount) {
    this.connection = connection;
    this.skierRidesMap = skierRidesMap;
    this.processedMessageCount = processedMessageCount;
  }

  @Override
  public void run() {
    try {
      Channel channel = connection.createChannel();
      channel.queueDeclare("skiersQueue", true, false, false, null);
      channel.basicQos(100);

      System.out.println(Thread.currentThread().getName() + " - [*] Waiting for messages...");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        try {
          LiftRide liftRide = gson.fromJson(message, LiftRide.class);

          skierRidesMap.computeIfAbsent(liftRide.getSkierID(), k -> new ConcurrentHashMap<>())
              .merge(liftRide.getDayID() + "-" + liftRide.getLiftID(), 1, Integer::sum);

          processedMessageCount.incrementAndGet();

          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (Exception e) {
          System.err.println("Error processing message: " + e.getMessage());
          channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
        }
      };
      channel.basicConsume("skiersQueue", false, deliverCallback, consumerTag -> {});
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class LiftRide {
  private int skierID;
  private int resortID;
  private int liftID;
  private String seasonID;
  private String dayID;
  private int time;

  public int getSkierID() { return skierID; }
  public void setSkierID(int skierID) { this.skierID = skierID; }

  public int getResortID() { return resortID; }
  public void setResortID(int resortID) { this.resortID = resortID; }

  public int getLiftID() { return liftID; }
  public void setLiftID(int liftID) { this.liftID = liftID; }

  public String getSeasonID() { return seasonID; }
  public void setSeasonID(String seasonID) { this.seasonID = seasonID; }

  public String getDayID() { return dayID; }
  public void setDayID(String dayID) { this.dayID = dayID; }

  public int getTime() { return time; }
  public void setTime(int time) { this.time = time; }
}