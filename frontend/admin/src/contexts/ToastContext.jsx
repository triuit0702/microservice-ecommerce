import { createContext, useContext, useState } from 'react'
//import CommonToast from '../components/common/CommonToast'
import CommonToast from '../views/common/CommonToast'

const ToastContext = createContext()

export const ToastProvider = ({ children }) => {
    const [toast, setToast] = useState({
        visible: false,
        message: '',
        color: 'success',
    })

    const showToast = (message, color = 'success') => {
        setToast({
            visible: true,
            message,
            color,
        })
    }

    return (
        <ToastContext.Provider value={{ showToast }}>
            {children}
            <CommonToast
                visible={toast.visible}
                setVisible={(v) => setToast({ ...toast, visible: v })}
                message={toast.message}
                color={toast.color}
            />
        </ToastContext.Provider>
    )
}

export const useToast = () => useContext(ToastContext)
