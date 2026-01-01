import { Navigate, useLocation } from 'react-router-dom'
import { useSelector, useDispatch } from 'react-redux'
import { useEffect } from 'react'
import { serviceFetchCurrentUser } from '../services/AuthService'

import LoadingScreen from '../views/common/LoadingScreen'

const PrivateRoute = ({ children }) => {
    const { user, loading } = useSelector((state) => state.auth)
    const dispatch = useDispatch();

    const token = localStorage.getItem("token");

    const location = useLocation();
    // if (["/login", "/403"].includes(location.pathname)) {
    //     return children
    // }

    useEffect(() => {

        if (user === null) {
            if (token) {
                dispatch({
                    type: 'SET_LOADING',
                    payload: true
                });
                serviceFetchCurrentUser()
                    .then(res => {

                        dispatch({
                            type: 'REFRESH',
                            payload: res.data.data,
                        })
                    })
                    .catch(() => {
                        dispatch({ type: 'LOGOUT' }) // nếu token không hợp lệ
                        localStorage.removeItem("token");
                    })
                    .finally(() => {
                        // 🔥 BẮT BUỘC
                        dispatch({ type: "SET_LOADING", payload: false });
                    });;
            } else {
                // không có token → logout để redirect
                dispatch({ type: 'LOGOUT' });
                localStorage.removeItem("token");
            }

        }
    }, [user, dispatch])

    if (loading) return <LoadingScreen />; // chờ fetch xong

    const isAuthenticated = !!user || !!token; // token còn thì vẫn hiển thị page

    // Chỉ redirect nếu user=null và token cũng không còn
    if (!isAuthenticated) return <Navigate to="/login" replace />;

    return children;
}

export default PrivateRoute
