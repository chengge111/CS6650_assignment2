import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class ChannelPool {
  private final GenericObjectPool<Channel> pool;

  public ChannelPool(Connection connection, String queueName) {
    GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(200);
    this.pool = new GenericObjectPool<>(new ChannelFactory(connection, queueName), config);
  }

  public Channel borrowChannel() throws Exception {
    return pool.borrowObject();
  }

  public void returnChannel(Channel channel) {
    if (channel != null) {
      try {
        pool.returnObject(channel);
      } catch (Exception e) {
        System.err.println("Error returning channel to pool: " + e.getMessage());
      }
    }
  }

  public void close() {
    try {
      pool.close();
    } catch (Exception e) {
      System.err.println("Error closing channel pool: " + e.getMessage());
    }
  }

  private static class ChannelFactory extends BasePooledObjectFactory<Channel> {
    private final Connection connection;
    private final String queueName;

    public ChannelFactory(Connection connection, String queueName) {
      this.connection = connection;
      this.queueName = queueName;
    }

    @Override
    public Channel create() throws Exception {
      Channel channel = connection.createChannel();
      channel.queueDeclare(queueName, true, false, false, null);
      return channel;
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
      return new DefaultPooledObject<>(channel);
    }

    @Override
    public boolean validateObject(PooledObject<Channel> p) {
      return p.getObject().isOpen();
    }

    @Override
    public void destroyObject(PooledObject<Channel> p) throws Exception {
      if (p.getObject().isOpen()) {
        p.getObject().close();
      }
    }
  }
}
