import { CSpinner } from '@coreui/react'

const LoadingScreen = () => {
    return (
        <div style={{
            position: 'fixed',
            top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.3)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 9999
        }}>
            <CSpinner color="primary" style={{ width: '3rem', height: '3rem' }} />
        </div>
    )
}

export default LoadingScreen;
