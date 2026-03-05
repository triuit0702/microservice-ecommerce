import {
    Drawer,
    Box,
    Typography,
    List,
    ListItem,
    ListItemText,
    Divider
} from "@mui/material";
import { useContext } from "react";
import { CartContext } from "../context/CartContext";

export default function CartDrawer({ open, onClose }) {
    const { cart } = useContext(CartContext);

    const total = cart.reduce((sum, item) => sum + item.price, 0);

    return (
        <Drawer anchor="right" open={open} onClose={onClose}>
            <Box sx={{ width: 300, p: 2 }}>
                <Typography variant="h6">Cart</Typography>

                <List>
                    {cart.map((item, index) => (
                        <ListItem key={index}>
                            <ListItemText
                                primary={item.title}
                                secondary={`$${item.price}`}
                            />
                        </ListItem>
                    ))}
                </List>

                <Divider />

                <Typography variant="h6" mt={2}>
                    Total: ${total.toFixed(2)}
                </Typography>
            </Box>
        </Drawer>
    );
}
