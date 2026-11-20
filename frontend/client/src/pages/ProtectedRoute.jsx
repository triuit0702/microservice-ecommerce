import { useEffect, useState } from "react";
import { serviceGetMe, serviceLogout } from "../services/AuthService";
import { isCustomer } from "../utils/roles";
import { Navigate, Outlet } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";


// use to check cookie user expired
const ProtectedRoute = () => {


    const [loading, setLoading] = useState(true)

    const dispatch = useDispatch();

    const user = useSelector((state) => state.auth.user);

    useEffect(() => {

        const checkAuth = async () => {
            try {
                const res = await serviceGetMe();

                // a staff session (e.g. cookie from the admin app) is not allowed in the client app
                if (!isCustomer(res.data.data)) {
                    await serviceLogout().catch(() => {});
                    dispatch({ type: 'LOGOUT', payload: null });
                    return;
                }

                dispatch({
                    type: 'LOGIN_SUCCESS',
                    payload: res.data.data
                })
            } catch (error) {
                console.log(error);

                dispatch({
                    type: 'LOGOUT',
                    payload: null
                })
            } finally {
                setLoading(false)
            }
        }

        checkAuth();

    }, []);

    if (loading) {
        return <div>Loading...</div>;
    }

    if (!user) {
        return <Navigate to="/login" replace />
    }

    return <Outlet />;


};

export default ProtectedRoute;