// src/store.js
import { configureStore } from "@reduxjs/toolkit";
import cartReducer from "./features/cartSlice";
import authReducer from "./features/authReducer";

import {
    persistStore,
    persistReducer,

    FLUSH,
    REHYDRATE,
    PAUSE,
    PERSIST,
    PURGE,
    REGISTER,
} from "redux-persist";

import storage from "redux-persist/lib/storage";

// config cho auth
const authPersistConfig = {
    key: "auth",
    storage,
};

// wrap auth reducer
const persistedAuthReducer = persistReducer(authPersistConfig, authReducer);


export const store = configureStore({
    reducer: {
        cart: cartReducer,
        // auth: authReducer
        auth: persistedAuthReducer
    },

    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware({
            serializableCheck: {
                ignoredActions: [
                    FLUSH,
                    REHYDRATE,
                    PAUSE,
                    PERSIST,
                    PURGE,
                    REGISTER,
                ],
            },
        }),


});

export const persistor = persistStore(store);





