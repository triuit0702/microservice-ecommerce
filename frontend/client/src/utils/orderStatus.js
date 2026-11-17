// Display helpers for order status / payment method / cancel reason

export const ORDER_STATUS = {
    PENDING: { label: "Pending", color: "info" },
    CONFIRMED: { label: "Confirmed", color: "success" },
    PROCESSING: { label: "Processing", color: "primary" },
    COMPLETED: { label: "Completed", color: "success" },
    CANCELLED: { label: "Cancelled", color: "error" },
};

export const getStatusInfo = (status) =>
    ORDER_STATUS[status] ?? { label: status ?? "Unknown", color: "default" };

export const PAYMENT_METHODS = {
    COD: "Cash on delivery",
};

export const getPaymentLabel = (method) => PAYMENT_METHODS[method] ?? method ?? "-";

// The backend already sends the cancel reason in English
export const getCancelReason = (reason) => reason ?? "Unknown reason";

export const formatMoney = (value) => `$${Number(value ?? 0).toFixed(2)}`;

export const formatDate = (value) => (value ? new Date(value).toLocaleString("en-US") : "-");

export const shortOrderId = (orderId) => (orderId ? orderId.slice(0, 8).toUpperCase() : "");
