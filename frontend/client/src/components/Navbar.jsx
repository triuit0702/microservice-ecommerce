import { AppBar, Toolbar, Typography, Badge, IconButton } from "@mui/material";
import ShoppingCartIcon from "@mui/icons-material/ShoppingCart";
import { useState } from "react";
import { CartContext } from "../context/CartContext";
import CartDrawer from "./CartDrawer";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";


export default function Navbar() {
    //    const { cart } = useContext(CartContext);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate();

    const items = useSelector((state) => state.cart.items);

    const totalQuantity = items.reduce(
        (sum, item) => sum + item.quantity,
        0
    );

    return (
        <>
            <AppBar position="static">
                <Toolbar>
                    <Typography
                        variant="h6"
                        sx={{ flexGrow: 1, cursor: "pointer" }}
                        onClick={() => navigate("/")}
                    >
                        Micro Store
                    </Typography>

                    <IconButton color="inherit" onClick={() => navigate("/cart")}>
                        <Badge badgeContent={totalQuantity} color="error">
                            <ShoppingCartIcon />
                        </Badge>
                    </IconButton>
                </Toolbar>
            </AppBar>

            {/* <CartDrawer open={open} onClose={() => setOpen(false)} /> */}
        </>
    );
}
