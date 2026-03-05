import { Card, CardContent, CardMedia, Typography, Button } from "@mui/material";
import { useNavigate } from "react-router-dom";

export default function ProductCard({ product }) {
    const navigate = useNavigate();

    return (
        <Card
            sx={{ height: "100%", display: "flex", flexDirection: "column", cursor: "pointer" }}
            onClick={() => navigate(`/products/${product.id}`)}
        >
            <CardMedia
                component="img"
                height="200"
                image={product.imageUrl}
                alt={product.title}
                sx={{ objectFit: "contain", p: 2 }}
            />

            <CardContent sx={{ flexGrow: 1 }}>
                <Typography
                    variant="subtitle1"
                    sx={{
                        width: "100%",            // 👈 thêm
                        maxWidth: "100%",
                        display: "-webkit-box",
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: "vertical",
                        overflow: "hidden",

                    }}
                >
                    {product.name}
                </Typography>

                <Typography variant="h6" color="primary" sx={{ mt: 1 }}>
                    ${product.price}
                </Typography>
            </CardContent>


        </Card>
    );
}
