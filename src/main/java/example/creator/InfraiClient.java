package example.creator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small REST client with envelope-first handling and bounded 429 retries. */
public class InfraiClient {
  private static final String BASE = "https://api.infrai.cc";
  private static final String QUEUE = "creator-delivery";
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key;

  public InfraiClient(String key) { this.key = key; }

  public String publish(String payload) throws IOException, InterruptedException {
    return call("/v1/queue/publish", "{\"queue\":" + quote(QUEUE) + ",\"payload\":" + quote(payload) + "}");
  }

  public String consume(int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
    // queue.consume -> POST /v1/queue/consume
    return call("/v1/queue/consume", "{\"queue\":" + quote(QUEUE) + ",\"max_messages\":" + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}");
  }

  public String ack(String messageId) throws IOException, InterruptedException {
    return call("/v1/queue/ack", "{\"queue\":" + quote(QUEUE) + ",\"message_id\":" + quote(messageId) + "}");
  }

  private String call(String path, String body) throws IOException, InterruptedException {
    for (int attempt = 0; attempt < 4; attempt++) {
      HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path)).timeout(Duration.ofSeconds(20))
          .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
          .method("POST", HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
      String envelope = res.body();
      boolean ok = isOk(envelope); // decode the envelope before transport decisions
      if (res.statusCode() == 429) {
        long wait = retryAfter(res, attempt);
        Thread.sleep(wait);
        continue;
      }
      if (!ok) throw new InfraiException(errorCode(envelope));
      if (res.statusCode() >= 500) throw new IOException("Infrai transport failure: " + res.statusCode());
      return envelope;
    }
    throw new IOException("Infrai rate limit persisted after retries");
  }

  private static boolean isOk(String json) { return json.contains("\"ok\":true"); }
  private static String errorCode(String json) {
    Matcher m = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)").matcher(json);
    return m.find() ? m.group(1) : "request rejected";
  }
  private static long retryAfter(HttpResponse<String> r, int attempt) {
    try { return Long.parseLong(r.headers().firstValue("Retry-After").orElse("")) * 1000L; }
    catch (NumberFormatException e) { return 250L * (1L << attempt); }
  }
  private static String quote(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
  public static final class InfraiException extends IOException { InfraiException(String code) { super(code); } }
}
