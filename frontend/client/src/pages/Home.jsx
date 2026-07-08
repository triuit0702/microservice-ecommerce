import { useEffect, useState } from "react";
import { Grid, CircularProgress, Alert, Box } from "@mui/material";
import { fetchProducts } from "../api/productApi";
import ProductCard from "../components/ProductCard";

export default function Home() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        fetchProducts()
            .then((data) => {
                setProducts(data)
            })
            .catch(() => setError("Failed to load products"))
            .finally(() => setLoading(false));
    }, []);

    if (loading) return <CircularProgress />;
    if (error) return <Alert severity="error">{error}</Alert>;
    return (

        <Box
            sx={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))",
                gap: 8
            }}
        >
            {products && products.length > 0 && products.map((p) => (
                <Grid item xs={12} sm={6} md={4} lg={3} zeroMinWidth key={p.id}>
                    <ProductCard product={p} />
                </Grid>
            ))}
        </Box>
    );
}
