### Place order flow (Saga with Kafka)

Orders and stock live in two services, each with its own database, so one order is split into small steps.
The services talk to each other through Kafka events, and a failed step is undone (the order is cancelled,
the stock is given back). This prevents selling more than the stock.

```
Client ──POST /api/v1/order──► order-service
                                  │ 1. quick check with product-service: products exist,
                                  │    enough stock, same prices (stock is not reduced yet)
                                  │ 2. save the order (PENDING) and the message to send
                                  │    in the same DB transaction (the "outbox")
                                  ▼
Client ◄── 3. 201 Created (order PENDING)

order-service (background job, every 1s)
   │ 4. send the saved message to Kafka
   ▼
[Kafka topic: inventory.command]
   ▼
product-service
   │ 5. reduce the stock → reply RESERVED (or CANCEL if out of stock)
   ▼
[Kafka topic: inventory.result]
   ▼
order-service
     6. RESERVED → order CONFIRMED,  CANCEL → order CANCELLED

Client polls GET /api/v1/order/{id} until the order is no longer PENDING.

If nothing happens for 5 minutes:
order-service (background job, every 60s)
   │ 7. order still PENDING → CANCELLED, send "release stock"
   ▼
[Kafka topic: inventory.command]
   ▼
product-service
     8. give the stock back
```

`RESERVED` / `CANCEL` are the replies from product-service; `CONFIRMED` / `CANCELLED` are the order statuses.

**Key points**
- **Idempotent checkout (safe to repeat):** each checkout sends an `Idempotency-Key`; a double click or a retry
  returns the same order instead of creating a second one.
- **Server prices win:** client prices are only compared; a difference returns `409 PRICE_CHANGED`.
- **Outbox:** the order and its message are saved together, so a message is never lost even if Kafka is down.
- **Duplicate-safe consumer:** product-service remembers the orders it has handled, so a repeated message never
  reduces the stock twice.

Details (error codes, topics, dead-letter topics, Kafka down, full sequence): [ORDER_FLOW_DETAILS.md](backend/ORDER_FLOW_DETAILS.md)
