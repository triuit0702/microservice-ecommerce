import { CSpinner } from '@coreui/react'

const FullPageLoader = () => {
    return (
        <div style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(255,255,255,0.6)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 9999
        }}>
            <CSpinner size="lg" />
        </div>
    )
}

export default FullPageLoader
