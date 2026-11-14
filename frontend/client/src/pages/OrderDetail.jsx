import {
    Alert, Box, Button, Chip, CircularProgress, Container, Divider, Grid, Paper, Step, StepLabel, Stepper,
    Table, TableBody, TableCell, TableHead, TableRow, Typography
} from "@mui/material";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate, useParams } from "react-router-dom";
import { getOrderByIdService } from "../services/CheckoutService";
import { getProductsByIdsService } from "../services/ProductService";
import { getCartByUserId } from "../services/CartService";
import { addListToCart, clearCart } from "../features/cartSlice";
import {
    formatDate, formatMoney, getCancelReason, getPaymentLabel, getStatusInfo, shortOrderId
} from "../utils/orderStatus";

const POLL_INTERVAL_MS = 1500;
const POLL_TIMEOUT_MS = 30000;

const STEPS = ["Đã đặt hàng", "Đã xác nhận", "Đang xử lý", "Hoàn thành"];
const STEP_INDEX = { PENDING: 0, CONFIRMED: 1, PROCESSING: 2, COMPLETED: 3 };

// key={id}: opening another order remounts the view, so its state starts fresh
export default function OrderDetail() {
    const { id } = useParams();
    return <OrderDetailView key={id} id={id} />;
}

function OrderDetailView({ id }) {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const user = useSelector(state => state.auth.user);

    const [order, setOrder] = useState(null);
    const [products, setProducts] = useState({});
    const [error, setError] = useState(null);
    const [timedOut, setTimedOut] = useState(false);

    // Load the order; while it is PENDING, poll until it gets a final status or the timeout is reached
    useEffect(() => {
        let cancelled = false;
        let timer = null;
        const startedAt = Date.now();

        const load = async () => {
            try {
                const res = await getOrderByIdService(id);
                if (cancelled) return;
                const data = res.data.data;
                setOrder(data);

                if (data.status === "PENDING") {
                    if (Date.now() - startedAt < POLL_TIMEOUT_MS) {
                        timer = setTimeout(load, POLL_INTERVAL_MS);
                    } else {
                        setTimedOut(true);
                    }
                }
            } catch (err) {
                if (cancelled) return;
                const status = err.response?.status;
                setError(status === 404
                    ? "Không tìm thấy đơn hàng"
                    : err.response?.data?.error?.message ?? "Không tải được đơn hàng, vui lòng thử lại");
            }
        };

        load();
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [id]);

    const status = order?.status;
    const isFinal = status && status !== "PENDING";

    // Backend updates the Redis cart once the order is final (items removed on success,
    // kept when cancelled), so re-sync the client cart badge at that point
    useEffect(() => {
        if (!isFinal || !user?.id) return;
        getCartByUserId(user.id).then((res) => {
            dispatch(clearCart());
            dispatch(addListToCart(res.data.items ?? []));
        }).catch((err) => console.log(err));
    }, [isFinal, user?.id, dispatch]);

    // Order items only carry ids; load name/image from product-service once
    const productIdsKey = order ? [...new Set(order.orderItems.map(i => i.productId))].sort().join(",") : "";
    useEffect(() => {
        if (!productIdsKey) return;
        getProductsByIdsService(productIdsKey.split(",")).then((res) => {
            const byId = {};
            (res.data.data ?? []).forEach(p => { byId[p.id] = p; });
            setProducts(byId);
        }).catch((err) => console.log(err));
    }, [productIdsKey]);

    const itemInfo = (item) => {
        const product = products[item.productId];
        const variant = product?.variants?.find(v => v.id === item.variantId);
        return {
            name: product?.name ?? item.productId,
            imageUrl: variant?.imageUrl ?? product?.imageUrl,
            variantLabel: [variant?.color ?? item.color, variant?.size].filter(Boolean).join(" / "),
        };
    };

    if (error) {
        return (
            <Container sx={{ mt: 4 }}>
                <Alert severity="error">{error}</Alert>
                <Button sx={{ mt: 2 }} variant="outlined" onClick={() => navigate("/orders")}>Đơn hàng của tôi</Button>
            </Container>
        );
    }

    if (!order) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
                <CircularProgress />
            </Box>
        );
    }

    const statusInfo = getStatusInfo(status);

    return (
        <Container sx={{ mb: 10 }}>
            {/* Status banner */}
            {status === "PENDING" && !timedOut && (
                <Alert severity="info" icon={<CircularProgress size={20} />}>
                    Đơn hàng đã được ghi nhận, đang kiểm tra tồn kho...
                </Alert>
            )}
            {status === "PENDING" && timedOut && (
                <Alert severity="warning">
                    Đơn hàng vẫn đang được xử lý. Bạn có thể xem lại trạng thái trong mục "Đơn hàng của tôi".
                </Alert>
            )}
            {status === "CONFIRMED" && (
                <Alert severity="success" icon={<CheckCircleOutlineIcon />}>
                    Đặt hàng thành công! Đơn hàng của bạn đã được xác nhận.
                </Alert>
            )}
            {status === "CANCELLED" && (
                <Alert
                    severity="error"
                    action={<Button color="inherit" size="small" onClick={() => navigate("/cart")}>Về giỏ hàng</Button>}
                >
                    Đơn hàng đã bị hủy: {getCancelReason(order.cancelReason)}. Sản phẩm vẫn còn trong giỏ hàng của bạn.
                </Alert>
            )}

            {/* Header */}
            <Paper sx={{ p: 2, mt: 2 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 1 }}>
                    <Box>
                        <Typography variant="h5">Đơn hàng #{shortOrderId(order.orderId)}</Typography>
                        <Typography variant="body2" color="text.secondary">Đặt lúc {formatDate(order.createdAt)}</Typography>
                    </Box>
                    <Chip label={statusInfo.label} color={statusInfo.color} />
                </Box>

                {status !== "CANCELLED" && (
                    <Stepper activeStep={STEP_INDEX[status] ?? 0} alternativeLabel sx={{ mt: 3 }}>
                        {STEPS.map(label => (
                            <Step key={label}><StepLabel>{label}</StepLabel></Step>
                        ))}
                    </Stepper>
                )}
            </Paper>

            <Grid container spacing={2} sx={{ mt: 0 }}>
                {/* Shipping info */}
                <Grid size={{ xs: 12, md: 6 }}>
                    <Paper sx={{ p: 2, height: "100%" }}>
                        <Typography variant="h6">Thông tin giao hàng</Typography>
                        <Typography>Người nhận: {order.receiverName ?? "-"}</Typography>
                        <Typography>Địa chỉ: {order.address ?? "-"}</Typography>
                        <Typography>Số điện thoại: {order.phone ?? "-"}</Typography>
                        <Typography>Email: {order.email ?? "-"}</Typography>
                    </Paper>
                </Grid>

                {/* Summary */}
                <Grid size={{ xs: 12, md: 6 }}>
                    <Paper sx={{ p: 2, height: "100%" }}>
                        <Typography variant="h6">Thanh toán</Typography>
                        <Typography>Phương thức: {getPaymentLabel(order.paymentMethod)}</Typography>
                        <Divider sx={{ my: 1 }} />
                        <Typography>Tạm tính: {formatMoney(order.subTotal)}</Typography>
                        <Typography>Phí ship: {formatMoney(order.shippingFee)}</Typography>
                        <Typography fontWeight="bold">Tổng cộng: {formatMoney(order.totalPrice)}</Typography>
                    </Paper>
                </Grid>
            </Grid>

            {/* Items */}
            <Paper sx={{ mt: 2, overflowX: "auto" }}>
                <Typography variant="h6" sx={{ p: 2 }}>Sản phẩm</Typography>
                <Table>
                    <TableHead>
                        <TableRow sx={{ backgroundColor: "grey.100" }}>
                            <TableCell align="center" width="15%">Ảnh</TableCell>
                            <TableCell width="35%">Tên sản phẩm</TableCell>
                            <TableCell align="center" width="15%">Giá</TableCell>
                            <TableCell align="center" width="15%">Số lượng</TableCell>
                            <TableCell align="right" width="20%">Thành tiền</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {order.orderItems.map((item) => {
                            const info = itemInfo(item);
                            return (
                                <TableRow key={item.id ?? `${item.productId}:${item.variantId}`}>
                                    <TableCell align="center">
                                        {info.imageUrl && (
                                            <Box
                                                component="img"
                                                src={info.imageUrl}
                                                alt={info.name}
                                                sx={{ width: 64, height: 64, objectFit: "cover", borderRadius: 1 }}
                                            />
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <Typography>{info.name}</Typography>
                                        {info.variantLabel && (
                                            <Typography variant="body2" color="text.secondary">{info.variantLabel}</Typography>
                                        )}
                                    </TableCell>
                                    <TableCell align="center">{formatMoney(item.price)}</TableCell>
                                    <TableCell align="center">{item.quantity}</TableCell>
                                    <TableCell align="right">{formatMoney(item.price * item.quantity)}</TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </Paper>

            <Box sx={{ display: "flex", gap: 2, mt: 3 }}>
                <Button variant="outlined" onClick={() => navigate("/orders")}>Đơn hàng của tôi</Button>
                <Button variant="contained" onClick={() => navigate("/")}>Tiếp tục mua sắm</Button>
            </Box>
        </Container>
    );
}
