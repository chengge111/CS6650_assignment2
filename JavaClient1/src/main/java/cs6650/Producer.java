package cs6650;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Producer implements Runnable {

    private final BlockingQueue<HttpRequest> buffer;
    private final int THREAD_COUNT;
    private final int threads;
    private final int maxRequests;

    public Producer(BlockingQueue<HttpRequest> buffer, int THREAD_COUNT, int threads, int maxRequests) {
        this.buffer = buffer;
        this.THREAD_COUNT = THREAD_COUNT;
        this.threads = threads;
        this.maxRequests = maxRequests;
    }

    @Override
    public void run() {
        for (int i = 0; i < maxRequests; i++) {
            buffer.add(produce());
        }
        for (int i = 0; i < threads + THREAD_COUNT; i++) {
            buffer.add(HttpRequest.newBuilder().uri(URI.create(RandomURI.getRandomURI())).build());
        }
    }

    private HttpRequest produce() {
        String message = "{ \"time\": %s, \"liftID\": %s}";
        message = String.format(message, ThreadLocalRandom.current().nextInt(1, 361), ThreadLocalRandom.current().nextInt(1, 41));
        return HttpRequest.newBuilder()
            .uri(URI.create(RandomURI.getRandomURI()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .build();
    }
}