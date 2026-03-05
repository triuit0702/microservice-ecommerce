import { Box, IconButton, Typography } from "@mui/material";
import RemoveIcon from "@mui/icons-material/Remove";
import AddIcon from "@mui/icons-material/Add";

export default function QuantitySelector({ value, onChange, min = 1, max = 20 }) {
    //  const [quantity, setQuantity] = useState(1);
    // const min = 1;
    //const max = 20;

    const handleDecrease = () => {
        //setQuantity((prev) => (prev > min ? prev - 1 : prev));
        if (value > min) onChange(value - 1);
    };

    const handleIncrease = () => {
        // setQuantity((prev) => (prev < max ? prev + 1 : prev));
        if (value < max) onChange(value + 1);
    };

    return (
        <Box
            sx={{
                display: "flex",
                alignItems: "center",
                border: "1px solid #ddd",
                borderRadius: 2,
                width: "fit-content",
                overflow: "hidden"
            }}
        >
            <IconButton
                onClick={handleDecrease}
                disabled={value === min}
                sx={{
                    borderRight: "1px solid #ddd",
                    borderRadius: 0,
                    px: 1.5
                }}
            >
                <RemoveIcon fontSize="small" />
            </IconButton>

            <Typography
                sx={{
                    width: 50,
                    textAlign: "center",
                    fontWeight: 500
                }}
            >
                {value}
            </Typography>

            <IconButton
                onClick={handleIncrease}
                disabled={value === max}
                sx={{
                    borderLeft: "1px solid #ddd",
                    borderRadius: 0,
                    px: 1.5
                }}
            >
                <AddIcon fontSize="small" />
            </IconButton>
        </Box>
    );
}