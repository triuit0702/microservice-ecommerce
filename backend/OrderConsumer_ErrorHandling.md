# Xử lý lỗi và retry trong `OrderConsumer` (product-service)

## 1. Luồng bình thường

`OrderConsumer.consume` nhận event đặt hàng từ `create_order_topic`:

1. Kiểm tra bảng `processed_orders` xem đơn đã được xử lý chưa .
2. Nếu chưa xử lý, gọi `InventoryService.reserveStock` , chạy trong **một transaction**:
   - INSERT `ProcessedOrder(orderId)`. Câu này chạy trước, để đánh dấu đơn đã xử lý.
   - Trừ stock bằng `UPDATE ... WHERE stockQuantity >= :quantity`. Nếu không đủ hàng thì ném `InsufficientStockException`.
3. Gửi kết quả `RESERVED` hoặc `CANCEL` sang order-service (`publishResult`).
4. Đánh dấu `published = true` (`markPublished`).

## 2. Kiến thức nền

- **Offset:** vị trí message trong Kafka. Consumer **commit offset** để báo "đã xử lý tới đây".
- **Retry của Spring Kafka (cấu hình mặc định):**
  - Hàm `consume` chạy xong bình thường → commit offset → message **không** được đọc lại.
  - Hàm `consume` ném exception → không commit, message được **đọc lại** (retry).
  - Tối đa 10 lần thử, không có delay giữa các lần. Hết 10 lần thì message bị bỏ qua.
- **Vì sao một message có thể bị xử lý 2 lần:** Kafka đảm bảo mỗi message được giao *ít nhất một lần*. Message có thể đến lại khi retry, khi consumer crash trước lúc commit, hoặc khi rebalance (Kafka chia lại partition giữa các consumer).
- **`@Transactional`:** khi method ném exception, Spring **rollback** trước, rồi exception mới truyền ra cho người gọi.

## 3. Chống xử lý trùng bằng `ProcessedOrder`

- `orderId` là khóa chính, và `ProcessedOrder.isNew()` luôn trả `true` nên Spring luôn INSERT. Vì vậy hai lần xử lý cùng đơn **không thể cùng lưu thành công**.
- Hai consumer C1 và C2 cùng nhận đơn A:
  1. C1 INSERT thành công và giữ lock trên khóa A.
  2. C2 INSERT cùng khóa nên bị MySQL bắt chờ C1.
  3. C1 commit, C2 bị lỗi trùng `DataIntegrityViolationException`. Transaction của C2 rollback; stock chưa bị trừ vì INSERT là câu đầu tiên.
  4. C2 vào `catch` (`OrderConsumer.java:74`), không gửi gì và không retry. C1 là bên gửi kết quả.
- Nếu C1 rollback (ví dụ hết hàng) thì INSERT của C2 thành công và C2 tự xử lý tiếp.

## 4. Các tình huống lỗi

| Tình huống | `consume` có ném lỗi? | Kết quả |
|---|---|---|
| Trùng `ProcessedOrder` khi `reserveStock` | Không | Bỏ qua, không gửi, không retry |
| Hết hàng hoặc dữ liệu đơn sai | Không | Rollback stock, lưu `CANCEL`, gửi `CANCEL` |
| Lỗi trùng trong `markCancelled` (cả C1, C2 cùng cancel) | Có | Retry; lần sau thấy `ProcessedOrder` nên không xử lý lại |
| Gửi kết quả thất bại | Có | Retry, gửi lại kết quả đã lưu, **không trừ stock lần nữa** |
| Gửi xong nhưng `markPublished` lỗi | Có | Retry, gửi trùng kết quả; order-service phải tự bỏ qua bản trùng |

Gửi thất bại retry được là nhờ `InventoryProducer.java:28` gọi `send(...).get()`. `.get()` chờ Kafka xác nhận và ném lỗi nếu thất bại. Nếu bỏ `.get()`, lỗi gửi sẽ không được báo về và kết quả bị mất.

## 5. Rủi ro còn lại (chưa sửa)

| # | Vấn đề | Hậu quả | Cách sửa đề xuất |
|---|---|---|---|
| 1 | Retry mặc định: 10 lần, không có delay | Kafka/DB lỗi vài giây là message bị bỏ qua, đơn kẹt `PENDING` | Cấu hình `DefaultErrorHandler` với `ExponentialBackOff` + `DeadLetterPublishingRecoverer` (đẩy message lỗi sang topic DLT) |
| 2 | `.get()` có thể chờ tới ~120s khi Kafka lỗi | Consumer bị chậm | Chỉnh `delivery.timeout.ms` nếu cần |
