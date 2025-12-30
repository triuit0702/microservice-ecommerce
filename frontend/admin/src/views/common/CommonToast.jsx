import {
    CToaster,
    CToast,
    CToastHeader,
    CToastBody,
} from '@coreui/react'

const CommonToast = ({
    visible,
    setVisible,
    message,
    color = 'success',
    title = 'Thông báo',
    delay = 3000,
}) => {
    return (
        <CToaster placement="top-end">
            <CToast
                visible={visible}
                autohide
                delay={delay}
                color={color}
                onClose={() => setVisible(false)}
            >
                <CToastHeader closeButton>
                    <strong className="me-auto">{title}</strong>
                </CToastHeader>
                <CToastBody>{message}</CToastBody>
            </CToast>
        </CToaster>
    )
}

export default CommonToast
