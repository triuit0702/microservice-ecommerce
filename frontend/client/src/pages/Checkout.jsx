import { Alert, Box, Button, Card, CardContent, Container, Divider, FormControlLabel, Grid, IconButton, List, ListItem, Paper, Radio, RadioGroup, Step, StepLabel, Stepper, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from "@mui/material";
import { getCartByUserId } from "../services/CartService";
import { useCallback, useEffect, useRef, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Controller, useForm } from "react-hook-form";
import CommonTextField from "../components/FormValidate/CommonTextField";
import { orderService } from "../services/CheckoutService";
import { addListToCart, clearCart } from "../features/cartSlice";

const SHIPPING_FEE = 10;

// Same rules as OrderRequestDto in order-service (backend still validates)
const PHONE_PATTERN = /^\+?[0-9]{8,15}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const FORM_FIELDS = ["address", "phone", "email", "paymentMethod"];

// crypto.randomUUID is only available in secure contexts (https / localhost)
const generateIdempotencyKey = () =>
    (window.crypto?.randomUUID?.()) ??
    `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;


export default function Checkout() {

    const getCheckoutForm = () => {
        const saved = sessionStorage.getItem("checkout_form");

        if (!saved) {
            return {
                address: "",
                phone: "",
                email: "",
                paymentMethod: "COD",
            };
        }

        return { paymentMethod: "COD", ...JSON.parse(saved) };
    };

    const { control, handleSubmit, setError } = useForm({
        defaultValues: getCheckoutForm()
    });

    const dispatch = useDispatch();
    const user = useSelector(state => state.auth.user);

    const [items, setItems] = useState([]);

    // One key per checkout attempt: kept across retries (a failed order is rolled back, so the key stays free),
    // a new one is generated when the page is opened again after a successful order.
    const idempotencyKeyRef = useRef(generateIdempotencyKey());
    const [submitting, setSubmitting] = useState(false);


    // error message 
    const [globalError, setGlobalError] = useState(null);

    const navigate = useNavigate();

    const CHECKOUT_FORM_KEY = "checkout_form";


    // Cart prices are joined from DB on every GET, so reloading shows the current prices
    const loadCart = useCallback(() => {
        if (!user?.id) return Promise.resolve();
        return getCartByUserId(user.id).then((res) => {
            const cartItems = res.data.items ?? [];
            setItems(cartItems);
            dispatch(clearCart());
            dispatch(addListToCart(cartItems));
        }).catch((err) => {
            console.log(err);
        });
    }, [user?.id, dispatch]);

    useEffect(() => {
        if (user?.id) {
            loadCart();
        } else {
            navigate(`/login`)
        }
    }, [user?.id, loadCart, navigate]);


    const totalPrice = items.reduce((total, item) => {
        return total + (item.price * item.quantity);
    }, 0)

    const grandTotal = totalPrice + SHIPPING_FEE;


    const orderItems = items.map(item => ({
        productId: item.id,
        quantity: item.quantity,
        price: item.price,
        variantId: item.variantId,
        color: item.color

    }))
    const submitOrder = async (data) => {
        if (submitting) return;
        setSubmitting(true);
        setGlobalError(null);

        // Prices the customer sees are sent; backend rejects with 409 if they no longer match DB prices
        const order = {
            ...data,
            receiverName: user.userName,
            subTotal: totalPrice,
            shippingFee: SHIPPING_FEE,
            totalPrice: grandTotal,
            orderItems
        };


        // keep info user temporary
        sessionStorage.setItem(
            CHECKOUT_FORM_KEY,
            JSON.stringify(data)
        );

        try {
            // execute submit
            const res = await orderService(order, idempotencyKeyRef.current);
            const orderId = res.data.data.orderId;

            // Ordered items are removed from the Redis cart by backend once stock is reserved;
            // here only the client state is cleared
            dispatch(
                clearCart()
            )

            // Thành công → xóa dữ liệu tạm
            sessionStorage.removeItem(CHECKOUT_FORM_KEY);

            // Order is PENDING here; the detail page polls until it is CONFIRMED / CANCELLED
            navigate(`/orders/${orderId}`, { replace: true })


        } catch (error) {
            const resError = error.response?.data?.error;
            if (resError?.code === "ORDER_IN_PROGRESS") {
                // Concurrent request with the same key is still being saved: retry with the SAME key
                // returns that order instead of creating a new one
                setGlobalError("Đơn hàng đang được xử lý, vui lòng bấm Đặt hàng lại sau giây lát.");
            } else if (resError?.code === "PRICE_CHANGED") {
                // Price changed: show new prices and let the customer confirm again.
                // The order content changed, so the next submit is a new checkout attempt (new key).
                await loadCart();
                idempotencyKeyRef.current = generateIdempotencyKey();
                setGlobalError(`Giá sản phẩm đã thay đổi, giỏ hàng đã được cập nhật giá mới. Vui lòng kiểm tra lại và đặt hàng. (${resError?.message ?? ""})`);
            } else if (resError?.code === "VALIDATION_FAILED") {
                // Backend field errors: show under the matching input, the rest at the top
                const fieldErrors = resError.details ?? {};
                const otherErrors = [];
                Object.entries(fieldErrors).forEach(([field, message]) => {
                    if (FORM_FIELDS.includes(field)) {
                        setError(field, { type: "server", message });
                    } else {
                        otherErrors.push(message);
                    }
                });
                setGlobalError(otherErrors.length
                    ? `Thông tin đặt hàng không hợp lệ: ${otherErrors.join(", ")}`
                    : "Thông tin đặt hàng không hợp lệ, vui lòng kiểm tra lại");
            } else {
                // Keep the same key so a retry never creates a duplicate order
                setGlobalError(resError?.message ?? "Đặt hàng thất bại, vui lòng thử lại");
            }
        } finally {
            setSubmitting(false);
        }

    }

    return (
        <form onSubmit={handleSubmit(submitOrder)}>
            <Container sx={{ mt: 0, mb: 10 }}>
                <Grid >


                    {/* ✅ Hiển thị lỗi ở đầu trang */}
                    {globalError && (
                        <Alert
                            severity="error"
                        //onClose={() => setFieldErrors({})}
                        >
                            <div >{globalError}</div>
                        </Alert>
                    )}

                    {/* Shipping Info đặt lên đầu */}
                    <Grid >
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h5">Thông tin giao hàng</Typography>
                            <CommonTextField
                                name="address"
                                control={control}
                                label="Địa chỉ"
                                rules={{
                                    validate: (v) => v?.trim() ? true : "Địa chỉ là bắt buộc",
                                    maxLength: { value: 255, message: "Địa chỉ tối đa 255 ký tự" }
                                }}
                            />
                            <CommonTextField
                                name="phone"
                                control={control}
                                label="Số điện thoại"
                                rules={{
                                    required: "Số điện thoại là bắt buộc",
                                    pattern: { value: PHONE_PATTERN, message: "Số điện thoại gồm 8-15 chữ số" }
                                }}
                            />
                            <CommonTextField
                                name="email"
                                control={control}
                                label="Email"
                                rules={{
                                    required: "Email là bắt buộc",
                                    pattern: { value: EMAIL_PATTERN, message: "Email không hợp lệ" },
                                    maxLength: { value: 255, message: "Email tối đa 255 ký tự" }
                                }}
                            />
                        </Paper>

                        {/* Cart Items */}


                        <Paper sx={{ mt: 2 }}>
                            <Typography variant="h5">Giỏ hàng</Typography>
                            <Table>
                                {/* header */}
                                <TableHead>
                                    <TableRow sx={{ backgroundColor: "grey.100" }}>
                                        <TableCell align="center" width="15%">Ảnh</TableCell>
                                        <TableCell width="35%">Tên sản phẩm</TableCell>
                                        <TableCell align="center" width="15%">Giá</TableCell>
                                        <TableCell align="center" width="15%">Số lượng</TableCell>
                                        <TableCell align="right" width="20%">Thành tiền</TableCell>
                                    </TableRow>
                                </TableHead>

                                {/* body */}

                                <TableBody>
                                    {items.map((item) => (
                                        <TableRow key={item.id}>

                                            {/* Image */}
                                            <TableCell align="center">
                                                <Box
                                                    component="img"
                                                    src={item.imageUrl}
                                                    alt={item.name}
                                                    sx={{
                                                        width: 80,
                                                        height: 80,
                                                        objectFit: "cover",
                                                        borderRadius: 1
                                                    }}
                                                />
                                            </TableCell>

                                            {/* Name */}
                                            <TableCell>
                                                <Typography variant="h6">
                                                    {item.name}
                                                </Typography>
                                            </TableCell>

                                            {/* Price */}
                                            <TableCell align="center">
                                                ${item.price}
                                            </TableCell>

                                            {/* Quantity */}
                                            <TableCell align="center">
                                                {item.quantity}
                                            </TableCell>

                                            {/* Total */}
                                            <TableCell align="right">
                                                ${(item.price * item.quantity).toFixed(2)}
                                            </TableCell>

                                        </TableRow>
                                    ))}
                                </TableBody>


                            </Table>

                        </Paper>

                        {/* Payment Method */}
                        <Paper sx={{ p: 2, mt: 2 }}>
                            <Typography variant="h5">Phương thức thanh toán</Typography>
                            <Controller
                                name="paymentMethod"
                                control={control}
                                render={({ field }) => (
                                    <RadioGroup {...field}>
                                        <FormControlLabel value="COD" control={<Radio />} label="Thanh toán khi nhận hàng" />
                                        {/* Not supported by backend yet */}
                                        <FormControlLabel value="CARD" control={<Radio />} label="Thẻ tín dụng (sắp có)" disabled />
                                        <FormControlLabel value="WALLET" control={<Radio />} label="Ví điện tử (sắp có)" disabled />
                                    </RadioGroup>
                                )}
                            />
                        </Paper>
                    </Grid>


                </Grid>

                {/* Order Summary */}
                <Grid sx={{ mt: 3, mb: 5 }} >
                    <Paper sx={{ p: 2 }}>
                        <Typography variant="h5">Tóm tắt đơn hàng</Typography>
                        <Typography>Tạm tính: ${totalPrice.toFixed(2)}</Typography>
                        <Typography>Phí ship: ${SHIPPING_FEE}</Typography>
                        <Typography>Tổng cộng: ${grandTotal.toFixed(2)} </Typography>
                        <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }} disabled={submitting || items.length === 0}>
                            {submitting ? "Đang xử lý..." : "Đặt hàng"}
                        </Button>
                    </Paper>
                </Grid>
            </Container>
        </form>
    )
}