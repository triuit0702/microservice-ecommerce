import { Box, Button, Card, CardContent, Container, Divider, FormControlLabel, Grid, IconButton, List, ListItem, Paper, Radio, RadioGroup, Step, StepLabel, Stepper, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from "@mui/material";
import { getCartByUserId } from "../services/CartService";
import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import CartQuantitySelector from "../components/CartQuantitySelector";



export default function Checkout() {

    const user = useSelector(state => state.auth.user);

    const [items, setItems] = useState([]);

    const [total, setTotal] = useState(0);

    const navigate = useNavigate();


    useEffect(() => {
        if (user?.id) {
            getCartByUserId(user.id).then((res) => {
                setItems(res.data.items);
                const totalVal = res.data.items.reduce((sum, item) => {
                    return sum + (item.price * item.quantity)
                }, 0);
                setTotal(totalVal);
            }).catch((err) => {
                console.log(err);
            })
        } else {
            navigate(`/login`)
        }

    }, [user]);


    return (
        <Container sx={{ mt: 0, mb: 10 }}>
            <Grid >


                {/* Shipping Info đặt lên đầu */}
                <Grid >
                    <Paper sx={{ p: 2 }}>
                        <Typography variant="h5">Thông tin giao hàng</Typography>
                        <TextField fullWidth label="Địa chỉ" margin="normal" />
                        <TextField fullWidth label="Số điện thoại" margin="normal" />
                        <TextField fullWidth label="Email" margin="normal" />
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
                                    <TableRow key={item.productId}>

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
                    <Typography>Tạm tính: 900k</Typography>
                    <Typography>Phí ship: 30k</Typography>
                    <Typography>Tổng cộng: 930k</Typography>
                    <Button variant="contained" color="primary" fullWidth sx={{ mt: 2 }}>
                        Đặt hàng
                    </Button>
                </Paper>
            </Grid>
        </Container>

    )
}