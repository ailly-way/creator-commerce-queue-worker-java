package example.creator;

import java.util.concurrent.atomic.AtomicReference;

public final class QueueWorkerTest {
  public static void main(String[] args) throws Exception {
    AtomicReference<CreatorJob> seen = new AtomicReference<>();
    FakeClient fake = new FakeClient();
    new QueueWorker(fake, 1000).runOnce(seen::set);
    if (!"asset-42".equals(seen.get().assetId()) || !fake.acked) throw new AssertionError("job must process before ack");
    System.out.println("QueueWorkerTest passed");
  }
  static final class FakeClient extends InfraiClient {
    boolean acked;
    FakeClient() { super("test"); }
    @Override public String consume(int a, int b) { return "{\"ok\":true,\"data\":{\"message_id\":\"m1\",\"job_id\":\"j1\",\"asset_id\":\"asset-42\",\"subscriber_id\":\"s1\",\"content\":\"ready\"}}"; }
    @Override public String ack(String id) { acked = true; return "{\"ok\":true}"; }
  }
}
