package example.creator;

/** Executable service entry point; configure one key and a conservative rate. */
public final class CreatorDeliveryService {
  public static void main(String[] args) throws Exception {
    String key = System.getenv("INFRAI_API_KEY");
    if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    InfraiClient client = new InfraiClient(key);
    client.publish(new CreatorJob("job-1001", "asset-42", "subscriber-7", "download-ready").payload());
    new QueueWorker(client, 2).runOnce(job -> System.out.println("delivered " + job.assetId() + " to " + job.subscriberId()));
  }
}
