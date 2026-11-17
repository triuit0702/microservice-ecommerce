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

            // success: clear temporary data
            sessionStorage.removeItem(CHECKOUT_FORM_KEY);

            // Order is PENDING here; the detail page polls until it is CONFIRMED / CANCELLED
            navigate(`/orders/${orderId}`, { replace: true })


        } catch (error) {
            const resError = error.response?.data?.error;
            if (resError?.code === "ORDER_IN_PROGRESS") {
                // Concurrent request with the same key is still being saved: retry with the SAME key
                // returns that order instead of creating a new one
                setGlobalError("Your order is still being processed. Please click Place order again in a moment.");
            } else if (resError?.code === "PRICE_CHANGED") {
                // Price changed: show new prices and let the customer confirm again.
                // The order content changed, so the next submit is a new checkout attempt (new key).
                await loadCart();
                idempotencyKeyRef.current = generateIdempotencyKey();
                setGlobalError(`Prices have changed and your cart now shows the new prices. Please review and place the order again. (${resError?.message ?? ""})`);
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
                    ? `Invalid order information: ${otherErrors.join(", ")}`
                    : "Invalid order information, please check again");
            } else {
                // Keep the same key so a retry never creates a duplicate order
                setGlobalError(resError?.message ?? "Could not place the order, please try again");
            }
        } finally {
            setSubmitting(false);
        }

    }

    return (
        <form onSubmit={handleSubmit(submitOrder)}>
            <Container sx={{ mt: 0, mb: 10 }}>
                <Grid >


                    {/* errors at the top of the page */}
                    {globalError && (
                        <Alert
                            severity="error"
                        //onClose={() => setFieldErrors({})}
                        >
                            <div >{globalError}</div>
                        </Alert>
                    )}

                    {/* shipping info first */}
                    <Grid >
                        <Paper sx={{ p: 2 }}>
                            <Typography variant="h5">Shipping information</Typography>
                            <CommonTextField
                                name="address"
                                control={control}
                                label="Address"
                                rules={{
                                    validate: (v) => v?.trim() ? true : "Address is required",
                                    maxLength: { value: 255, message: "Address must be at most 255 characters" }
                                }}
                            />
                            <CommonTextField
                                name="phone"
                                control={control}
                                label="Phone number"
                                rules={{
                                    required: "Phone number is required",
                                    pattern: { value: PHONE_PATTERN, message: "Phone number must have 8-15 digits" }
                                }}
                            />
                            <CommonTextField
                                name="email"
                                control={control}
                                label="Email"
                                rules={{
                                    required: "Email is required",
                                    pattern: { value: EMAIL_PATTERN, message: "Invalid email" },
                                    maxLength: { value: 255, message: "Email must be at most 255 characters" }
                                }}
                            />
                        </Paper>

                        {/* Cart Items */}


                        <Paper sx={{ mt: 2 }}>
                            <Typography variant="h5">Cart</Typography>
                            <Table>
                                {/* header */}
                                <TableHead>
                                    <TableRow sx={{ backgroundColor: "grey.100" }}>
                                        <TableCell align="center" width="15%">Image</TableCell>
                                        <TableCell width="35%">Product</TableCell>
                                        <TableCell align="center" width="15%">Price</TableCell>
                                        <TableCell align="center" width="15%">Quantity</TableCell>
                                        <TableCell align="right" width="20%">Total</TableCell>
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
                            <Typography variant="h5">Payment method</Typography>
                            <Controller
                                name="paymentMethod"
                                control={control}
                                render={({ field }) => (
                                    <RadioGroup {...field}>
                                        <FormControlLabel value="COD" control={<Radio />} label="Cash on delivery" />
                                        {/* Not supported by backend yet */}
                                        <FormControlLabel value="CARD" control={<Radio />} label="Credit card (coming soon)" disabled />
                                        <FormControlLabel value="WALLET" control={<Radio />} label="E-wallet (coming soon)" disabled />
                                    </RadioGroup>
                                )}
                            />
                        </Paper>
                    </Grid>


                </Grid>

                {/* Order Summary */}
                <Grid sx={{ mt: 3, mb: 5 }} >
                    <Paper sx={{ p: 2 }}>
                        <Typography variant="h5">Order summary</Typography>
                        <Typography>Subtotal: ${totalPrice.toFixed(2)}</Typography>
                        <Typography>Shipping: ${SHIPPING_FEE}</Typography>
                        <Typography>Total: ${grandTotal.toFixed(2)} </Typography>
                        <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }} disabled={submitting || items.length === 0}>
                            {submitting ? "Processing..." : "Place order"}
                        </Button>
                    </Paper>
                </Grid>
            </Container>
        </form>
    )
}