import {
    Alert, Box, Button, Chip, CircularProgress, Container, Pagination, Paper,
    Table, TableBody, TableCell, TableHead, TableRow, Typography
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getMyOrdersService } from "../services/CheckoutService";
import { formatDate, formatMoney, getStatusInfo, shortOrderId } from "../utils/orderStatus";

const PAGE_SIZE = 10;

export default function MyOrders() {
    const navigate = useNavigate();
    // Page kept in the URL (1-based) so going back from an order detail keeps the page
    const [searchParams, setSearchParams] = useSearchParams();
    const page = Math.max(Number(searchParams.get("page")) || 1, 1);

    // Result of the last loaded page; loading while it is not the requested page
    const [result, setResult] = useState({ page: null, orders: [], totalPages: 0, error: null });
    const loading = result.page !== page;
    const { orders, totalPages, error } = result;

    useEffect(() => {
        let cancelled = false;

        getMyOrdersService({ page: page - 1, size: PAGE_SIZE })
            .then((res) => {
                if (cancelled) return;
                const data = res.data.data;
                setResult({ page, orders: data?.content ?? [], totalPages: data?.totalPages ?? 0, error: null });
            })
            .catch((err) => {
                if (cancelled) return;
                setResult({
                    page, orders: [], totalPages: 0,
                    error: err.response?.data?.error?.message ?? "Không tải được danh sách đơn hàng"
                });
            });

        return () => { cancelled = true; };
    }, [page]);

    const itemCount = (order) => order.orderItems?.reduce((sum, i) => sum + i.quantity, 0) ?? 0;

    return (
        <Container sx={{ mb: 10 }}>
            <Typography variant="h5" sx={{ mb: 2 }}>Đơn hàng của tôi</Typography>

            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", mt: 6 }}>
                    <CircularProgress />
                </Box>
            ) : orders.length === 0 && !error ? (
                <Paper sx={{ p: 4, textAlign: "center" }}>
                    <Typography sx={{ mb: 2 }}>Bạn chưa có đơn hàng nào.</Typography>
                    <Button variant="contained" onClick={() => navigate("/")}>Mua sắm ngay</Button>
                </Paper>
            ) : (
                <Paper sx={{ overflowX: "auto" }}>
                    <Table>
                        <TableHead>
                            <TableRow sx={{ backgroundColor: "grey.100" }}>
                                <TableCell>Mã đơn</TableCell>
                                <TableCell>Ngày đặt</TableCell>
                                <TableCell align="center">Số sản phẩm</TableCell>
                                <TableCell align="right">Tổng tiền</TableCell>
                                <TableCell align="center">Trạng thái</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {orders.map((order) => {
                                const statusInfo = getStatusInfo(order.status);
                                return (
                                    <TableRow
                                        key={order.orderId}
                                        hover
                                        sx={{ cursor: "pointer" }}
                                        onClick={() => navigate(`/orders/${order.orderId}`)}
                                    >
                                        <TableCell>#{shortOrderId(order.orderId)}</TableCell>
                                        <TableCell>{formatDate(order.createdAt)}</TableCell>
                                        <TableCell align="center">{itemCount(order)}</TableCell>
                                        <TableCell align="right">{formatMoney(order.totalPrice)}</TableCell>
                                        <TableCell align="center">
                                            <Chip size="small" label={statusInfo.label} color={statusInfo.color} />
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </Paper>
            )}

            {totalPages > 1 && (
                <Box sx={{ display: "flex", justifyContent: "center", mt: 3 }}>
                    <Pagination
                        count={totalPages}
                        page={page}
                        onChange={(_, value) => setSearchParams({ page: String(value) })}
                        color="primary"
                    />
                </Box>
            )}
        </Container>
    );
}
