package cs6650;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsumerInitial implements Runnable {

    private static final int REQUESTS_PER_THREAD = 1000;
    private final HttpClient httpClient;
    private final CountDownLatch completed1;
    private final CountDownLatch completed2;
    private final AtomicInteger successCounter;
    private final AtomicInteger failedCounter;
    private final BlockingQueue<HttpRequest> buffer;

    public ConsumerInitial(BlockingQueue<HttpRequest> buffer, HttpClient httpClient, CountDownLatch completed1, CountDownLatch completed2, AtomicInteger successCounter, AtomicInteger failedCounter) {
        this.buffer = buffer;
        this.httpClient = httpClient;
        this.completed1 = completed1;
        this.completed2 = completed2;
        this.successCounter = successCounter;
        this.failedCounter = failedCounter;
    }

    @Override
    public void run() {
        for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
            try {
                HttpRequest request = buffer.take();
                if (request.method().equals("GET")) {
                    break;
                }
                consume(request);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        if (completed1.getCount() > 0) {
            completed1.countDown();
        }
        completed2.countDown();
    }

    private void consume(HttpRequest httpRequest) {
        try {
            long start = System.currentTimeMillis();
            HttpResponse<String> res = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long end = System.currentTimeMillis();
            Log.updateCSV(start, end, res.statusCode());
            successCounter.incrementAndGet();
            System.out.println(res.statusCode());
            System.out.println(res.body());
        } catch (IOException | InterruptedException e) {
            failedCounter.incrementAndGet();
            throw new RuntimeException(e);
        }
    }
}
