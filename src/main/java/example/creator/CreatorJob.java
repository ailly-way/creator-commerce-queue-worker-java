package example.creator;

public record CreatorJob(String jobId, String assetId, String subscriberId, String content) {
  public String payload() {
    return "{\"job_id\":\"" + jobId + "\",\"asset_id\":\"" + assetId + "\",\"subscriber_id\":\"" + subscriberId + "\",\"content\":\"" + content + "\"}";
  }
}
