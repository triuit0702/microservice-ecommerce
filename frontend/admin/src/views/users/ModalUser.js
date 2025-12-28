/* eslint-disable prettier/prettier */
import { Modal } from '@coreui/coreui'
import { CButton, CFormInput, CModal, CModalBody, CModalFooter, CModalHeader } from '@coreui/react'

export default function ModalUser({
    visible,
    editingUser,
    onClose,
    userDetail
}) {
    return (


        <CModal visible={visible} onClose={onClose}>
            <CModalHeader>{editingUser ? 'Sửa User' : 'Thêm User'}</CModalHeader>
            <CModalBody>
                <CFormInput
                    label="Username"
                    value={(userDetail && editingUser) ? userDetail.username : ''}
                    className="mb-3"
                />

                <CFormInput
                    label="Email"
                    value={(userDetail && editingUser) ? userDetail.email : ''}
                />
            </CModalBody>

            <CModalFooter>
                <CButton color="secondary" >
                    Hủy
                </CButton>
                <CButton color="primary" >
                    Lưu
                </CButton>
            </CModalFooter>
        </CModal>
    )
}
