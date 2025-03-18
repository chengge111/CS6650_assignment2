package cs6650;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private final BlockingQueue<HttpRequest> buffer;
    private final HttpClient httpClient;
    private final CountDownLatch completed;
    private final AtomicInteger successCounter;
    private final AtomicInteger failedCounter;
    private final int maxRequests;

    public Consumer(BlockingQueue<HttpRequest> buffer, HttpClient httpClient, CountDownLatch completed, AtomicInteger successCounter, AtomicInteger failedCounter, int maxRequests) {
        this.buffer = buffer;
        this.httpClient = httpClient;
        this.completed = completed;
        this.successCounter = successCounter;
        this.failedCounter = failedCounter;
        this.maxRequests = maxRequests;
    }

    @Override
    public void run() {
        while (successCounter.get() < maxRequests) {
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
        completed.countDown();
    }

    private void consume(HttpRequest httpRequest) {
        try {
            long start = System.currentTimeMillis();
            HttpResponse<String> res = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long end = System.currentTimeMillis();
            Log.updateCSV(start, end, res.statusCode());
            successCounter.incrementAndGet();
            //System.out.println(res.statusCode());
            //System.out.println(res.body());
        } catch (IOException | InterruptedException e) {
            failedCounter.incrementAndGet();
            throw new RuntimeException(e);
        }
    }
}
