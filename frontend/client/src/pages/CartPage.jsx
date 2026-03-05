// src/pages/CartPage.jsx
import {
    Container,
    Grid,
    Typography,
    Card,
    CardContent,
    Box,
    TextField,
    IconButton,
    Button,
    Paper,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { useSelector, useDispatch } from "react-redux";
import {
    updateQuantity,
    removeItem,
    selectSubtotal,
} from "../features/cartSlice";
import QuantitySelector from "../components/QuantitySelector";

export default function CartPage() {
    const items = useSelector((state) => state.cart.items);
    const subtotal = useSelector(selectSubtotal);
    const dispatch = useDispatch();

    if (items.length === 0) {
        return (
            <Container sx={{ mt: 5 }}>
                <Typography variant="h5" align="center">
                    Your cart is empty
                </Typography>
            </Container>
        );
    }

    return (
        <Container sx={{ mt: 5 }}>
            <Typography variant="h4" gutterBottom>
                Shopping Cart
            </Typography>

            <Grid container spacing={4}>
                {/* LEFT SIDE - CART ITEMS */}
                <Grid item xs={12} md={8}>
                    {items.map((item) => (
                        <Card key={item.id} sx={{ mb: 2 }}>
                            <CardContent sx={{ display: "flex", gap: 3, alignItems: "center" }}>
                                <Box
                                    component="img"
                                    src={item.image}
                                    alt={item.name}
                                    sx={{ width: 100, height: 100, objectFit: "cover" }}
                                />

                                <Box sx={{ flex: 1 }}>
                                    <Typography variant="h6">{item.name}</Typography>
                                    <Typography color="text.secondary">
                                        ${item.price}
                                    </Typography>
                                </Box>

                                {/* <TextField
                                    type="number"
                                    size="small"
                                    value={item.quantity}
                                    onChange={(e) =>
                                        dispatch(
                                            updateQuantity({
                                                id: item.id,
                                                quantity: Number(e.target.value),
                                            })
                                        )
                                    }
                                    sx={{ width: 80 }}
                                    inputProps={{ min: 1 }}
                                /> */}


                                {/* quantity */}
                                <QuantitySelector
                                    value={item.quantity}
                                    onChange={(e) =>
                                        dispatch(
                                            updateQuantity({
                                                id: item.id,
                                                quantity: Number(e.target.value),
                                            })
                                        )}
                                />


                                <Typography sx={{ width: 100, textAlign: "right" }}>
                                    ${(item.price * item.quantity).toFixed(2)}
                                </Typography>

                                <IconButton
                                    color="error"
                                    onClick={() => dispatch(removeItem(item.id))}
                                >
                                    <DeleteIcon />
                                </IconButton>
                            </CardContent>
                        </Card>
                    ))}
                </Grid>

                {/* RIGHT SIDE - SUMMARY */}
                <Grid item xs={12} md={4}>
                    <Paper sx={{ p: 3 }}>
                        <Typography variant="h6" gutterBottom>
                            Order Summary
                        </Typography>

                        <Box
                            sx={{ display: "flex", justifyContent: "space-between", mb: 2 }}
                        >
                            <Typography>Subtotal</Typography>
                            <Typography>${subtotal.toFixed(2)}</Typography>
                        </Box>

                        <Button
                            variant="contained"
                            fullWidth
                            size="large"
                            disabled={subtotal === 0}
                        >
                            Checkout
                        </Button>
                    </Paper>
                </Grid>
            </Grid>
        </Container>
    );
}