import { legacy_createStore as createStore, combineReducers } from 'redux'

const initialState = {
  sidebarShow: true,
  theme: 'light',
}

const changeState = (state = initialState, { type, ...rest }) => {
  switch (type) {
    case 'set':
      return { ...state, ...rest }
    default:
      return state
  }
}

// auth reducer
const authInitialState = {
  user: null,
  loading: true // true while the app checks the session on start
}

const authReducer = (state = authInitialState, action) => {
  switch (action.type) {
    case 'LOGIN_SUCCESS':
      return { ...state, user: action.payload, loading: false }
    case 'REFRESH':
      return { ...state, user: action.payload, loading: false };

    case 'LOGOUT':
      return { ...state, user: null, loading: false };
    case 'SET_LOADING':
      return { ...state, loading: action.payload }

    default:
      return state
  }
}

const rootReducer = combineReducers({
  changeState: changeState,
  auth: authReducer,
})

const store = createStore(rootReducer)
export default store
