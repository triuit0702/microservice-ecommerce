import { useLocation, Routes, Route } from "react-router-dom";
import { Container } from "@mui/material";
import Navbar from "./components/Navbar";

import React from "react";
import ProtectedRoute from "./pages/ProtectedRoute";


const Home = React.lazy(() => import("./pages/Home"));
const ProductDetail = React.lazy(() => import("./pages/ProductDetail"));
const CartPage = React.lazy(() => import("./pages/CartPage"));
const Login = React.lazy(() => import("./pages/Login"))
const Register = React.lazy(() => import("./pages/Register"))
const Checkout = React.lazy(() => import("./pages/Checkout"))
const OrderDetail = React.lazy(() => import("./pages/OrderDetail"))
const MyOrders = React.lazy(() => import("./pages/MyOrders"))

function App() {
  const location = useLocation();
  const hideNavbar = location.pathname === "/login";





  return (
    <>
      {!hideNavbar && <Navbar />}
      {/* <Container maxWidth="md" sx={{ mt: 4 }}> */}
      <Container maxWidth={false} sx={{ mt: 4 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/products/:id" element={<ProductDetail />} />



          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />


          {/* <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<Checkout />} />

          <Route path="/order-success" element={<OrderSuccess />} /> */}


          {/* Cần login */}
          <Route element={<ProtectedRoute />}>

            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<Checkout />} />

            {/* Order status is async (PENDING -> CONFIRMED / CANCELLED): detail page polls it */}
            <Route path="/orders" element={<MyOrders />} />
            <Route path="/orders/:id" element={<OrderDetail />} />
          </Route>


        </Routes>
      </Container>
    </>
  );
}

export default App;
