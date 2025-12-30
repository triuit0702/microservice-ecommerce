import { Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'

const PrivateRoute = ({ children }) => {
    const user = useSelector((state) => state.auth.user)
    const isAuthenticated = !!user

    return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default PrivateRoute
