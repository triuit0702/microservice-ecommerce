
//import { REHYDRATE } from 'redux-persist';


// auth reducer mới
const authInitialState = {
    user: null,
    loading: true // loangding = true lúc start app
}

const authReducer = (state = authInitialState, action) => {
    switch (action.type) {
        case 'LOGIN_SUCCESS':
            return { ...state, user: action.payload, loading: false }
        case 'REFRESH':
            return { ...state, user: action.payload, loading: false };

        case 'LOGOUT':
            return { ...state, user: null, loading: false };
        // case 'SET_LOADING':
        //     return { ...state, loading: action.payload }

        // case REHYDRATE:
        //     const incoming = action.payload?.auth;
        //     if (incoming) {
        //         return { ...state, ...incoming, loading: false };
        //     }
        //     return state;
        default:
            return state
    }
}

export default authReducer;
