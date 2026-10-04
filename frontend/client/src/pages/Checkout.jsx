import { Alert, Box, Button, Card, CardContent, Container, Divider, FormControlLabel, Grid, IconButton, List, ListItem, Paper, Radio, RadioGroup, Step, StepLabel, Stepper, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from "@mui/material";
import { deleteCartByUserId, getCartByUserId } from "../services/CartService";
import { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import CommonTextField from "../components/FormValidate/CommonTextField";
import { orderService } from "../services/CheckoutService";
import { clearCart } from "../features/cartSlice";


export default function Checkout() {

    const getCheckoutForm = () => {
        const saved = sessionStorage.getItem("checkout_form");

        if (!saved) {
            return {
                address: "",
                phone: "",
                email: "",
            };
        }

        return JSON.parse(saved);
    };

    const { control, handleSubmit } = useForm({
        defaultValues: getCheckoutForm()
    });

    const dispatch = useDispatch();
    const user = useSelector(state => state.auth.user);

    const [items, setItems] = useState([]);


    // error message 
    const [globalError, setGlobalError] = useState(null);

    const navigate = useNavigate();

    const CHECKOUT_FORM_KEY = "checkout_form";


    useEffect(() => {
        if (user?.id) {
            getCartByUserId(user.id).then((res) => {
                setItems(res.data.items);

            }).catch((err) => {
                console.log(err);
            })
        } else {
            navigate(`/login`)
        }

    }, [user]);


    const totalPrice = items.reduce((total, item) => {
        return total + (item.price * item.quantity);
    }, 0)

    const grandTotal = totalPrice + 10;


    const orderItems = items.map(item => ({
        productId: item.id,
        quantity: item.quantity,
        price: item.price,
        variantId: item.variantId,
        color: item.color

    }))
    const submitOrder = async (data) => {
        const order = {
            ...data,
            receiverName: user.userName,
            subTotal: totalPrice,
            shippingFee: 10,
            grandTotal: grandTotal,
            orderItems
        };


        // keep info user temporary
        sessionStorage.setItem(
            CHECKOUT_FORM_KEY,
            JSON.stringify(data)
        );

        try {
            // execute submit
            await orderService(order);

            // clear cart in state react
            dispatch(
                clearCart()
            )

            // Thành công → xóa dữ liệu tạm
            sessionStorage.removeItem(CHECKOUT_FORM_KEY);


            // delete list cart item in redis by userId
            await deleteCartByUserId(user.id);
            navigate(`/order-success`)


        } catch (error) {
            let resError = error.response.data.error;
            setGlobalError(resError.message);
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
                                rules={{ required: "Địa chỉ là bắt buộc" }}
                            />
                            <CommonTextField
                                name="phone"
                                control={control}
                                label="Số điện thoại"
                                rules={{ required: "Số đien thoại là bắt buộc" }}
                            />
                            <CommonTextField
                                name="email"
                                control={control}
                                label="Email"
                                rules={{ required: "Email là bắt buộc" }}
                            />}
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
                            <RadioGroup>
                                <FormControlLabel value="cod" control={<Radio />} label="Thanh toán khi nhận hàng" />
                                <FormControlLabel value="card" control={<Radio />} label="Thẻ tín dụng" />
                                <FormControlLabel value="wallet" control={<Radio />} label="Ví điện tử" />
                            </RadioGroup>
                        </Paper>
                    </Grid>


                </Grid>

                {/* Order Summary */}
                <Grid sx={{ mt: 3, mb: 5 }} >
                    <Paper sx={{ p: 2 }}>
                        <Typography variant="h5">Tóm tắt đơn hàng</Typography>
                        <Typography>Tạm tính: ${totalPrice.toFixed(2)}</Typography>
                        <Typography>Phí ship: $10</Typography>
                        <Typography>Tổng cộng: ${grandTotal.toFixed(2)} </Typography>
                        <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }}>
                            Đặt hàng
                        </Button>
                    </Paper>
                </Grid>
            </Container>
        </form>
    )
}