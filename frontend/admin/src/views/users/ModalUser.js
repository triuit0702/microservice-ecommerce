
import { Modal } from '@coreui/coreui'
import { CButton, CFormInput, CFormLabel, CFormSelect, CInputGroup, CInputGroupText, CModal, CModalBody, CModalFooter, CModalHeader } from '@coreui/react'
import { useEffect, useState } from 'react'

export default function ModalUser({
    visible,
    editingUser,
    hideModal,
    userDetail,
    setUserDetail,
    saveUser
}) {

    const [formData, setFormData] = useState({
        id: null,
        name: "",
        email: "",
        roleId: ""
    })



    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value })
        // let copyForm = { ...formData}
        // copyForm[]
    }

    const onClose = () => {
        setFormData({
            id: null,
            name: "",
            email: "",
            roleId: ""
        })
        hideModal()

    }

    useEffect(() => {
        if (userDetail) setFormData(userDetail)
    }, [userDetail])

    return (


        <CModal visible={visible} onClose={onClose}>
            <CModalHeader>{editingUser ? 'Sửa User' : 'Thêm User'}</CModalHeader>
            <CModalBody>
                <CFormInput
                    name="name"
                    label="Username"
                    value={formData.name}
                    onChange={handleChange}
                    className="mb-3"
                />

                <CFormInput
                    name="email"
                    label="Email"
                    value={formData.email}
                    onChange={handleChange}
                />

                <CFormLabel className="mt-2">Role</CFormLabel>
                <CFormSelect
                    name="roleId"
                    value={formData.roleId}
                    onChange={handleChange}
                >
                    <option value="">-- Select role --</option>
                    <option value="1">Admin</option>
                    <option value="2">Employee</option>
                    <option value="3">Customer</option>
                </CFormSelect>



            </CModalBody>

            <CModalFooter>
                <CButton color="secondary" >
                    Hủy
                </CButton>
                <CButton color="primary" onClick={() => saveUser(formData)} >
                    Lưu
                </CButton>
            </CModalFooter>
        </CModal>
    )
}
