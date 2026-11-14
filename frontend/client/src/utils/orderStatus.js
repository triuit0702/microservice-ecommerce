// Display helpers for order status / payment method / cancel reason

export const ORDER_STATUS = {
    PENDING: { label: "Đang xác nhận", color: "info" },
    CONFIRMED: { label: "Đã xác nhận", color: "success" },
    PROCESSING: { label: "Đang xử lý", color: "primary" },
    COMPLETED: { label: "Hoàn thành", color: "success" },
    CANCELLED: { label: "Đã hủy", color: "error" },
};

export const getStatusInfo = (status) =>
    ORDER_STATUS[status] ?? { label: status ?? "Không rõ", color: "default" };

export const PAYMENT_METHODS = {
    COD: "Thanh toán khi nhận hàng",
};

export const getPaymentLabel = (method) => PAYMENT_METHODS[method] ?? method ?? "-";

// Reasons sent by backend (English) -> Vietnamese; unknown reasons are shown as is
const CANCEL_REASONS = {
    "Some items are out of stock": "Một số sản phẩm đã hết hàng",
    "Some items are no longer available": "Một số sản phẩm không còn được bán",
};

export const getCancelReason = (reason) => CANCEL_REASONS[reason] ?? reason ?? "Không rõ lý do";

export const formatMoney = (value) => `$${Number(value ?? 0).toFixed(2)}`;

export const formatDate = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "-");

export const shortOrderId = (orderId) => (orderId ? orderId.slice(0, 8).toUpperCase() : "");
