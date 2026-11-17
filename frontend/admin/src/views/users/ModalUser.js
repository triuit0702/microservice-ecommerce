
import { CButton, CFormInput, CFormLabel, CFormSelect, CModal, CModalBody, CModalFooter, CModalHeader } from '@coreui/react'
import { useState } from 'react'
import { STAFF_ROLES } from '../../constants/permissions'

const INIT_FORM_DATA = {
    id: null,
    name: "",
    email: "",
    password: "",
    roleCode: ""
}

// Customer accounts keep CUSTOMER (customers and staff are separate groups)
export const isCustomerAccount = (user) => !!user?.roles?.includes('CUSTOMER')

const toFormData = (userDetail) => {
    if (!userDetail) return INIT_FORM_DATA
    return {
        ...INIT_FORM_DATA,
        id: userDetail.id,
        name: userDetail.name ?? "",
        email: userDetail.email ?? "",
        roleCode: userDetail.roles?.[0] ?? ""
    }
}

export default function ModalUser({
    visible,
    editingUser,
    hideModal,
    userDetail,
    roles,
    saveUser
}) {

    const [formData, setFormData] = useState(INIT_FORM_DATA)

    // reset the form when the modal opens or the user changes (state update during render instead of useEffect)
    const [prevProps, setPrevProps] = useState({ visible, userDetail })
    if (prevProps.visible !== visible || prevProps.userDetail !== userDetail) {
        setPrevProps({ visible, userDetail })
        if (visible) setFormData(toFormData(userDetail))
    }

    const customerAccount = editingUser && isCustomerAccount(userDetail)
    // admin only creates/edits staff accounts; a customer's role is shown read-only
    const roleOptions = customerAccount
        ? (roles || []).filter((role) => role.code === 'CUSTOMER')
        : (roles || []).filter((role) => STAFF_ROLES.includes(role.code))

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value })
    }

    const onClose = () => {
        setFormData(INIT_FORM_DATA)
        hideModal()
    }

    return (

        <CModal visible={visible} onClose={onClose} size="lg">
            <CModalHeader>{editingUser ? 'Edit User' : 'Add User'}</CModalHeader>
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
                    className="mb-3"
                />

                {/* password only on create; editing does not change it */}
                {!editingUser && (
                    <CFormInput
                        name="password"
                        type="password"
                        label="Password"
                        autoComplete="new-password"
                        value={formData.password}
                        onChange={handleChange}
                    />
                )}

                <CFormLabel className="mt-2">Role</CFormLabel>
                <CFormSelect
                    name="roleCode"
                    value={formData.roleCode}
                    onChange={handleChange}
                    disabled={customerAccount}
                >
                    <option value="">-- Select role --</option>
                    {roleOptions.map((role) => (
                        <option key={role.code} value={role.code}>{role.label}</option>
                    ))}
                </CFormSelect>

            </CModalBody>

            <CModalFooter>
                <CButton color="secondary" onClick={onClose}>
                    Cancel
                </CButton>
                <CButton color="primary" onClick={() => saveUser(formData, customerAccount)} >
                    Save
                </CButton>
            </CModalFooter>
        </CModal>
    )
}
