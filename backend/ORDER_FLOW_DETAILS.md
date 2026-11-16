### Place order flow – details

Short overview: [PLACE_ORDER_FLOW.md](../PLACE_ORDER_FLOW.md). Consumer error handling: [OrderConsumer_ErrorHandling.md](OrderConsumer_ErrorHandling.md).

#### Idempotent checkout

The client sends an `Idempotency-Key` header (one per checkout attempt, max 64 chars).
The pair `(user_id, idempotency_key)` is unique in `orders`, so a double click or a retry returns the existing order
instead of creating a second one. Two concurrent requests with the same key: the loser gets the unique-constraint
error and returns the winner's order (or `409 ORDER_IN_PROGRESS` on a lock timeout, safe to retry).

#### Pre-check (fail fast, before T1)

order-service loads the products by Feign (`GET /api/v1/product/products/by-ids`, outside any DB transaction) and
checks them itself in `OrderServiceImpl.validateStockAndPrice`: items are well-formed (1–50 items, quantity 1–100),
all products and variants exist, stock is enough per variant, and the client prices (and shipping fee) equal the
server prices. The order is always saved with server prices. This is only a pre-check: the real stock reservation is T2.

| Case                                                 | Response                |
| ---------------------------------------------------- | ----------------------- |
| Missing `Idempotency-Key` or longer than 64 chars    | `400`                   |
| Invalid items / not enough stock                     | `400`                   |
| Client price or shipping fee differs from the server | `409 PRICE_CHANGED`     |
| Same key still being processed (lock timeout)        | `409 ORDER_IN_PROGRESS` |
| product-service unavailable / returns no data        | `503` / `502`           |

#### Timeout + stock release

`PendingOrderTimeoutJob` (every 60s, batch 100) cancels orders still `PENDING` after 5 minutes and, in the same
transaction, writes a `RELEASE_STOCK` outbox event. product-service gives the stock back only if `processed_orders`
is `RESERVED` (switched to `RELEASED`, so at most once); otherwise it does nothing and sends no reply.
A `RESERVED` result arriving for an already cancelled order also triggers `RELEASE_STOCK`.
The timeout job and the inventory result can update the same order at the same time; the `@Version` column makes
one of them fail with an optimistic lock error, and the job simply skips that order.

#### Topics

| Topic (config key)                   | Name                | Producer → Consumer             | Payload                                                     |
| ------------------------------------ | ------------------- | ------------------------------- | ----------------------------------------------------------- |
| `app.kafka.topics.inventory-command` | `inventory.command` | order-service → product-service | `OrderMessageEvent` (`CREATE_ORDER` / `RELEASE_STOCK`, items) |
| `app.kafka.topics.inventory-result`  | `inventory.result`  | product-service → order-service | `OrderMessageEvent` (orderId, `RESERVED` / `CANCEL`, reason)  |

**Dead-letter topics:** both consumers use `ErrorHandlingDeserializer` and a `DefaultErrorHandler`
(`KafkaErrorHandlingConfig` in each service). A record that keeps failing (3 retries, 1s apart) or cannot be
deserialized is moved to the DLT of its topic, on the same partition number, so it never blocks the records behind it.

| DLT                     | Written by      | Contains                                                  |
| ----------------------- | --------------- | --------------------------------------------------------- |
| `inventory.command.DLT` | product-service | `CREATE_ORDER` / `RELEASE_STOCK` that could not be handled |
| `inventory.result.DLT`  | order-service   | `RESERVED` / `CANCEL` results that could not be handled    |

DLT records are not replayed automatically; check them in Kafka UI and resend by hand. A `CREATE_ORDER` in the DLT
leaves the order `PENDING`, so the timeout job cancels it; a `RELEASE_STOCK` in the DLT must be replayed, otherwise
the stock stays reserved.

**Topic creation – dev vs production:** in dev, each service declares the topics it writes to (`NewTopic` beans,
partition count from `app.kafka.topics.partitions`): order-service declares `inventory.command` and
`inventory.result.DLT`, product-service declares `inventory.result` and `inventory.command.DLT`. A DLT must have the
same partition count as its source topic. The replication factor is left to the broker. In production, topics would
be managed as infrastructure (Terraform / Strimzi `KafkaTopic`) with replication factor 3, `min.insync.replicas=2`
and producers using `acks=all`; auto-creation is then turned off with `KAFKA_ADMIN_AUTO_CREATE=false`.

#### When Kafka is down

Placing an order does not touch Kafka, so the request still returns `201` with a `PENDING` order and a `NEW` outbox
event. `OutboxPublisherJob` retries every run (each attempt blocks up to the 5s ACK timeout) and stops the batch at
the first failure, so events keep their order. When Kafka is back, producers and consumers reconnect by themselves
and the order completes within seconds. If Kafka stays down more than 5 minutes, the timeout job cancels the order.
An outbox event whose payload cannot be parsed is skipped and stays `NEW` (fix it by hand).

Known limitations (end state stays correct thanks to `processed_orders`):
- Each failed send while Kafka is down leaves a copy in the producer buffer, which is still delivered later,
  so product-service may receive the same `CREATE_ORDER` several times and skips the duplicates.
- An order cancelled by the timeout job keeps its `CREATE_ORDER` outbox event `NEW`; it is still sent when Kafka is
  back, so stock is reserved and then released again.

#### Full sequence

```
Client ──POST /api/v1/order (Idempotency-Key)──► order-service / OrderController
                                  │ 1. same key already used? → return that order
                                  │ 2. Feign → product-service (GET products/by-ids), then check items,
                                  │      stock and price (no DB transaction held; invalid → 400,
                                  │      price differs → 409 PRICE_CHANGED, product-service down → 503)
                                  │ 3. One DB transaction (T1):
                                  │      - save Order (PENDING, server prices)
                                  │      - save OutboxEvent CREATE_ORDER (NEW)
                                  ▼
◄── 4. 201 Created (order PENDING)

OutboxPublisherJob (every 1s, batch 500)
   │ 5. read NEW events → send to Kafka (key = orderId), wait for broker ACK (5s) → mark SENT
   │    send fails → stop this run, retry next run (Kafka down: order stays PENDING)
   ▼
inventory.command
   ▼
product-service / OrderConsumer (T2)
   │ 6. RELEASE_STOCK? → RESERVED → RELEASED + give stock back, no reply. Stop.
   │ 7. orderId already in processed_orders?
   │      published = true  → skip (duplicate message)
   │      published = false → resend the stored result (step 9)
   │ 8. one transaction: insert processed_orders (RESERVED) + decrease stock with
   │    conditional UPDATE (stock >= quantity), variants sorted by id
   │      not enough stock → rollback, insert processed_orders (CANCEL)
   │ 9. send RESERVED / CANCEL, wait for ACK → processed_orders.published = true
   │ 10. RESERVED → remove ordered items from the Redis cart (best effort)
   │     error after 3 retries / bad record → inventory.command.DLT
   ▼
inventory.result
   ▼
order-service / InventoryConsumer (T3)
     11. order PENDING:   RESERVED → CONFIRMED, CANCEL → CANCELLED (+ reason)
         order CANCELLED + RESERVED → outbox RELEASE_STOCK
         anything else (duplicate, unknown status) → ignored
         error after 3 retries / bad record → inventory.result.DLT

PendingOrderTimeoutJob (every 60s)
     PENDING > 5 min → CANCELLED + outbox RELEASE_STOCK (same transaction)
```
