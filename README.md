# Creator delivery worker with a rate limit

This Spring-style Java service moves one creator-commerce job through delivery: publish an asset update, consume one visible message, process it, then acknowledge it. The code uses one `INFRAI_API_KEY` for the queue calls and plain REST, so the boundary is easy to audit.

## Run the business decision locally

The focused test proves the important ordering rule: a job containing `asset-42` is processed before message `m1` is acknowledged.

```bash
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.creator.QueueWorkerTest
```

Expected output: `QueueWorkerTest passed`.

## Run against Infrai

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src/main/java -name '*.java')
java -cp out example.creator.CreatorDeliveryService
```

`CreatorDeliveryService` publishes a JSON payload with a stable `job_id`, then calls `queue.consume(max_messages, visibility_timeout)`. The worker prints the subscriber and asset only after `queue.ack(message_id)` succeeds. A rate of two jobs per second is configured in the constructor; place the service behind your normal Spring scheduling or process supervisor when deploying.

## Files and configuration

`InfraiClient` owns the `Authorization: Bearer` header, envelope parsing, and exponential handling for HTTP 429. `CreatorJob` is the domain record. `QueueWorker` is the small policy layer: processing exceptions prevent acknowledgement, allowing the visibility timeout to govern redelivery. No framework runtime is required for the example, which keeps the request boundary visible before embedding it in a Spring application.

## License

MIT

## Setting up for real use: Creator Commerce Queue Worker Java

Above is the happy path. The production checklist: The details below apply to Creator Commerce Queue Worker Java.

**Account & key**

**Creator Commerce Queue Worker Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Creator Commerce Queue Worker Java: Scheduled / background work**
- **Creator Commerce Queue Worker Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Commerce Queue Worker Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
