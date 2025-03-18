import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "SkierServlet", urlPatterns = "/skiers/*")
public class SkierServlet extends HttpServlet {
  private static final String QUEUE_NAME = "skiersQueue";
  private ChannelPool channelPool;
  private final Gson gson = new Gson();

  @Override
  public void init() throws ServletException {
    int maxConnections = 200;
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.25.112");
    factory.setUsername("admin");
    factory.setPassword("admin123");

    try {
      Connection conn = factory.newConnection();
      channelPool = new ChannelPool(conn, QUEUE_NAME); // initialize ChannelPool
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException("Failed to establish RabbitMQ connection.", e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String path = req.getPathInfo();  //get url pathInfo, extract special info

    // URL valid?
    if (path == null || path.isEmpty() || !isValidURL(path.split("/"))) {
      sendResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL");
      return;
    }

    // JSON data
    Map<String, Object> skierData;
    try (BufferedReader reader = req.getReader()) {
      skierData = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
    } catch (JsonSyntaxException e) {
      sendResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
      return;
    }

    // test is json valid
    if (!isValidSkier(skierData)) {
      sendResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid skier data");
      return;
    }

    // send messages to RabbitMQ
    try {
      Channel channel = channelPool.borrowChannel();
      skierData.put("resortID", path.split("/")[1]);
      skierData.put("seasonID", path.split("/")[3]);
      skierData.put("dayID", path.split("/")[5]);
      skierData.put("skierID", path.split("/")[7]);

      String message = gson.toJson(skierData);
      //System.out.println(message);
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
      channelPool.returnChannel(channel);

      sendResponse(res, HttpServletResponse.SC_CREATED, message);
    } catch (Exception e) {
      sendResponse(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to send message");
    }
  }

  // is URL valid
  private boolean isValidURL(String[] parts) {
    return parts.length == 8 && "seasons".equals(parts[2]) && "days".equals(parts[4]) && "skiers".equals(parts[6]);
  }

  //  is JSON data valid
  private boolean isValidSkier(Map<String, Object> skierData) {
    if (!skierData.containsKey("time") || !skierData.containsKey("liftID")) return false;
    int time = ((Number) skierData.get("time")).intValue();
    int liftID = ((Number) skierData.get("liftID")).intValue();
    return time >= 1 && time <= 360 && liftID >= 1 && liftID <= 40;
  }

  // json respond closure
  private void sendResponse(HttpServletResponse res, int status, String msg) throws IOException {
    res.setStatus(status);
    res.setContentType("application/json");
    res.getWriter().print(String.format("{\"message\":\"%s\"}", msg));
    res.getWriter().flush();
  }
}
