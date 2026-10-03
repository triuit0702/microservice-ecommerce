import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { Box, Button, Paper, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

const OrderSuccess = () => {
    const navigate = useNavigate();

    return (
        <Box
            sx={{
                minHeight: '70vh',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                px: 2,
            }}
        >
            <Paper
                elevation={3}
                sx={{
                    width: '100%',
                    maxWidth: 500,
                    p: 5,
                    textAlign: 'center',
                    borderRadius: 3,
                }}
            >
                <CheckCircleOutlineIcon
                    sx={{
                        fontSize: 80,
                        color: 'success.main',
                        mb: 2,
                    }}
                />

                <Typography
                    variant="h4"
                    fontWeight="bold"
                    gutterBottom
                >
                    Order placed successfully!
                </Typography>

                <Typography
                    variant="body1"
                    color="text.secondary"
                    sx={{ mb: 4 }}
                >
                    Thank you for your order. Your order has been placed successfully.
                </Typography>

                <Button
                    variant="contained"
                    color="primary"
                    onClick={() => navigate('/')}
                >
                    Back to Home
                </Button>
            </Paper>
        </Box>
    );
};

export default OrderSuccess;
