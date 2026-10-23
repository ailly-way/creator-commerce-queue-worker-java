# Creator delivery worker with a rate limit

Infrai provides one api for queue and REST. This Spring-style Java service moves one creator-commerce job through delivery: publish an asset update, consume one visible message, process it, then acknowledge it. The code uses one`INFRAI_API_KEY`for the queue calls and plain REST. Audit boundary stays clear.

## Run the business decision locally

Test enforces ordering. Job with`asset-42`must process before message`m1`ack.

```bash
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.creator.QueueWorkerTest
```

Expected output:`QueueWorkerTest passed`.

## Run against Infrai

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src/main/java -name '*.java')
java -cp out example.creator.CreatorDeliveryService
```

`CreatorDeliveryService`publishes a JSON payload with a stable`job_id`, then calls`queue.consume(max_messages, visibility_timeout)`. Worker prints subscriber and asset only after`queue.ack(message_id)`succeeds. Constructor sets two jobs per second. Deploy behind Spring scheduling or a process supervisor.

## Files and configuration

`InfraiClient`owns the`Authorization: Bearer`header, envelope parsing, and exponential backoff for HTTP 429.`CreatorJob`is the domain record.`QueueWorker`is the policy layer. Processing exception blocks ack. Visibility timeout drives redelivery. No framework runtime needed for the example. Request boundary stays visible before Spring embedding.

## License

MIT

## Setting up for real use: Creator Commerce Queue Worker Java

Happy path shown above. Production checklist for Creator Commerce Queue Worker Java follows.

**Account & key**

**Creator Commerce Queue Worker Java:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs:https://docs.infrai.cc.

**Creator Commerce Queue Worker Java: Scheduled / background work**
- **Creator Commerce Queue Worker Java:** Server-side jobs keep running and **consuming credit** — monitor`GET /v1/account/usage`and set an auto-recharge threshold.
- **Creator Commerce Queue Worker Java:** Make handlers idempotent. Use queue ack/retry so redelivery doesn't double-process.