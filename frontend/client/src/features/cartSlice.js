import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    items: [],
};

const cartSlice = createSlice({
    name: "cart",
    initialState,
    reducers: {
        addToCart(state, action) {
            const existing = state.items.find(
                (i) => i.id === action.payload.id
            );
            if (existing) {
                existing.quantity += 1;
            } else {
                state.items.push(action.payload);
            }
        },
        updateQuantity(state, action) {
            const { id, quantity } = action.payload;
            const item = state.items.find((i) => i.id === id);
            if (item && quantity >= 1) {
                item.quantity = quantity;
            }
        },
        removeItem(state, action) {
            state.items = state.items.filter(
                (i) => i.id !== action.payload
            );
        },
    },
});

export const {
    addToCart,
    updateQuantity,
    removeItem,
} = cartSlice.actions;

export default cartSlice.reducer;

export const selectSubtotal = (state) =>
    state.cart.items.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    );