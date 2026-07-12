import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    items: [],
};

const cartSlice = createSlice({
    name: "cart",
    initialState,
    reducers: {
        /**
         * state : đang lưu list product cũ  ??? chưa hiểu lắm cần check lai 
         * còn action.payload là product mới (là dự liệu bạn truyển vào khi dispatch)
         * @param {*} state 
         * @param {*} action 
         */
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
        addListToCart(state, action) {
            action.payload.forEach(item => {
                const existing = state.items.find(i => i.id === item.id);

                if (existing) {
                    existing.quantity += item.quantity || 1;
                } else {
                    state.items.push(item);
                }
            });
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
                (i) => i.id !== action.payload.id
            );
        },
        clearCart(state) {
            state.items = [];
        }
    },
});

export const {
    addToCart,
    addListToCart,
    updateQuantity,
    removeItem,
    clearCart
} = cartSlice.actions;

export default cartSlice.reducer;

export const selectSubtotal = (state) =>
    state.cart.items.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    );