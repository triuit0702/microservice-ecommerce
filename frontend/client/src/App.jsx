import { useLocation, Routes, Route } from "react-router-dom";
import { Container } from "@mui/material";
import Navbar from "./components/Navbar";

import React, { useEffect } from "react";
import { useDispatch } from 'react-redux';
import { serviceGetMe } from "./services/AuthService";


const Home = React.lazy(() => import("./pages/Home"));
const ProductDetail = React.lazy(() => import("./pages/ProductDetail"));
const CartPage = React.lazy(() => import("./pages/CartPage"));
const Login = React.lazy(() => import("./pages/Login"))
const Checkout = React.lazy(() => import("./pages/Checkout"))

function App() {
  const location = useLocation();
  const hideNavbar = location.pathname === "/login";

  const dispatch = useDispatch();




  useEffect(() => {


    serviceGetMe().then((res) => {

      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: res.data.data
      })
    }).catch((err) => {
      console.log(err);
      dispatch({
        type: 'LOGOUT',
        payload: null
      })
    })


  }, []);

  // useEffect(() => {
  //   // call api to get user from cookie
  // },[])

  return (
    <>
      {!hideNavbar && <Navbar />}
      {/* <Container maxWidth="md" sx={{ mt: 4 }}> */}
      <Container maxWidth={false} sx={{ mt: 4 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/products/:id" element={<ProductDetail />} />

          <Route path="/cart" element={<CartPage />} />

          <Route path="/login" element={<Login />} />
          <Route path="/checkout" element={<Checkout />} />
        </Routes>
      </Container>
    </>
  );
}

export default App;
