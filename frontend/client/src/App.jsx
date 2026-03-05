import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Container } from "@mui/material";
import Navbar from "./components/Navbar";
import Home from "./pages/Home";
import ProductDetail from "./pages/ProductDetail";
import CartPage from "./pages/CartPage";

function App() {
  return (
    <>
      <Navbar />
      {/* <Container maxWidth="md" sx={{ mt: 4 }}> */}
      <Container maxWidth={false} sx={{ mt: 4 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/products/:id" element={<ProductDetail />} />

          <Route path="/cart" element={<CartPage />} />
        </Routes>
      </Container>
    </>
  );
}

export default App;
