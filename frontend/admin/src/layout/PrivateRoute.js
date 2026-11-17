import { Navigate } from 'react-router-dom'
import { useSelector, useDispatch } from 'react-redux'
import { useEffect } from 'react'
import { serviceFetchCurrentUser, serviceLogout } from '../services/AuthService'
import { isStaff } from '../constants/permissions'

import LoadingScreen from '../views/common/LoadingScreen'

// Login state comes only from /auth/me (auth cookies are httpOnly).
// 200 -> logged in; 401 -> AxiosClient tries /auth/refresh once, then we go to /login.
const PrivateRoute = ({ children }) => {
    const { user, loading } = useSelector((state) => state.auth)
    const dispatch = useDispatch()

    useEffect(() => {
        if (user !== null) {
            return
        }
        let cancelled = false
        dispatch({ type: 'SET_LOADING', payload: true })
        serviceFetchCurrentUser()
            .then(async (res) => {
                if (cancelled) return
                const currentUser = res.data.data
                if (!isStaff(currentUser)) {
                    // customer accounts cannot use the admin app
                    await serviceLogout().catch(() => {})
                    dispatch({ type: 'LOGOUT' })
                    return
                }
                dispatch({ type: 'REFRESH', payload: currentUser })
            })
            .catch(() => {
                if (!cancelled) dispatch({ type: 'LOGOUT' })
            })
        return () => { cancelled = true }
    }, []) // eslint-disable-line react-hooks/exhaustive-deps

    if (loading) return <LoadingScreen />

    if (!user) return <Navigate to="/login" replace />

    return children
}

export default PrivateRoute
