package example.creator;

import java.util.function.Consumer;

/** Rate-limited consumer: delivery is acknowledged only after processing succeeds. */
public final class QueueWorker {
  private final InfraiClient client;
  private final long intervalMillis;
  public QueueWorker(InfraiClient client, int perSecond) {
    this.client = client; this.intervalMillis = 1000L / Math.max(1, perSecond);
  }
  public void runOnce(Consumer<CreatorJob> processor) throws Exception {
    String response = client.consume(1, 30);
    String messageId = field(response, "message_id");
    if (messageId == null) return;
    CreatorJob job = new CreatorJob(field(response, "job_id"), field(response, "asset_id"), field(response, "subscriber_id"), field(response, "content"));
    processor.accept(job);
    client.ack(messageId);
    Thread.sleep(intervalMillis);
  }
  private static String field(String json, String name) {
    String needle = "\"" + name + "\":\""; int start = json.indexOf(needle); if (start < 0) return null;
    int from = start + needle.length(), end = json.indexOf('"', from); return end < 0 ? null : json.substring(from, end);
  }
}
