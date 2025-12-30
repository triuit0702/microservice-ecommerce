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

// auth reducer mới
const authInitialState = {
  user: null,
}

const authReducer = (state = authInitialState, action) => {
  switch (action.type) {
    case 'LOGIN_SUCCESS':
      return { ...state, user: action.payload }
    case 'LOGOUT':
      return { ...state, user: null }
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
