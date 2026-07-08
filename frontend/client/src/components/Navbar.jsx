import { AppBar, Toolbar, Typography, Badge, IconButton } from "@mui/material";
import ShoppingCartIcon from "@mui/icons-material/ShoppingCart";
import { useNavigate, useLocation } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import React, { useEffect } from "react";
import { getCartByUserId } from "../services/CartService";

import { addListToCart, clearCart } from "../features/cartSlice";

import { Menu, MenuItem, Avatar } from "@mui/material";
import LogoutIcon from "@mui/icons-material/Logout";
import { serviceLogout } from "../services/AuthService";
import { Login } from "@mui/icons-material";



export default function Navbar() {
    const navigate = useNavigate();

    const items = useSelector((state) => state.cart.items);

    const user = useSelector(state => state.auth.user);

    const dispatch = useDispatch();

    const location = useLocation();
    // Ẩn nút nếu đang ở /cart, hiện lại khi ra trang khác
    const hideCart = location.pathname === "/cart";

    const totalQuantity = items.reduce(
        (sum, item) => sum + item.quantity,
        0
    );

    // state for menu
    const [anchorEl, setAnchorEl] = React.useState(null);

    const handleOpenMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleCloseMenu = () => {
        setAnchorEl(null);
    };

    const handleLogout = async () => {
        await serviceLogout();

        // clear user iinfo1
        dispatch({
            type: 'LOGOUT',
        })
        //navigate("/login");
    };

    const handleLogin = () => {
        navigate("/login");
    }

    useEffect(() => {
        if (user?.id) {
            getCartByUserId(user.id).then((response) => {

                dispatch(clearCart());

                dispatch(
                    addListToCart(response.data.items)
                )
            }).catch((err) => {
                console.error("Error fetching cart:", err);
            });
        }



    }, [user?.id]);


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

                    {!user && (
                        <>
                            <IconButton color="inherit"
                                sx={{
                                    "&:hover": {
                                        backgroundColor: "rgba(0,0,0,0.1)" // màu nền đậm hơn khi hover
                                    }
                                }}
                                onClick={handleLogin}>
                                <Login sx={{ mr: 1 }} />
                                <Typography variant="body1">Login</Typography>

                            </IconButton>
                        </>
                    )}


                    {user && user.id &&
                        (
                            <>


                                <IconButton
                                    color="inherit"
                                    onClick={() => navigate("/cart")}
                                    sx={{ display: hideCart ? "none" : "inline-flex" }}
                                >
                                    <Badge badgeContent={totalQuantity} color="error">
                                        <ShoppingCartIcon />
                                    </Badge>
                                </IconButton>



                                <IconButton color="inherit" onClick={handleOpenMenu}>
                                    <Avatar>
                                        {user?.userName?.charAt(0)?.toUpperCase()}
                                    </Avatar>
                                </IconButton>

                                {/* Dropdown menu */}
                                <Menu
                                    anchorEl={anchorEl}
                                    open={Boolean(anchorEl)}
                                    onClose={handleCloseMenu}
                                >

                                    <MenuItem >
                                        xin chào: {user.userName}
                                    </MenuItem>
                                    <MenuItem onClick={() => navigate("/profile")}>
                                        Profile
                                    </MenuItem>



                                    <MenuItem onClick={handleLogout}>
                                        <LogoutIcon sx={{ mr: 1 }} />
                                        Logout
                                    </MenuItem>
                                </Menu>
                            </>
                        )
                    }





                </Toolbar>
            </AppBar>

        </>
    );
}
