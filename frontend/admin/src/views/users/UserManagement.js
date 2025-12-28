/* eslint-disable prettier/prettier */

import { useState } from 'react'
import {
    CButton,
    CCard,
    CCardBody,
    CCardHeader,
    CTable,
    CTableBody,
    CTableDataCell,
    CTableHead,
    CTableHeaderCell,
    CTableRow,
} from '@coreui/react'
import ModalAddUser from './ModalUser'

export default function UserManagement() {
    const [users, setUsers] = useState([
        { id: 1, username: 'admin', email: 'admin@gmail.com', description: 'aaa' },
        { id: 2, username: 'user1', email: 'user1@gmail.com', description: 'aaa' },
    ])

    const [visibleModal, setVisibleModal] = useState(false);
    const [editingUser, setEditingUser] = useState(false);
    const [userDetail, setUserDetail] = useState(null);

    function openAdd() {
        console.log("open ad modal");
        setVisibleModal(true);
        setEditingUser(false);

        // console.log("visible: ", visibleModal); ham set state chay bat dong bo nên ko có in ra ket qua liền
    }

    function openEdit(item) {
        console.log("open edit user modal");
        setVisibleModal(true);
        setEditingUser(true);
        setUserDetail(item);
    }

    return (
        <CCard>
            <ModalAddUser
                visible={visibleModal}
                editingUser={editingUser}
                onClose={() => setVisibleModal(false)}
                userDetail={userDetail}
            />

            <CCardHeader className="d-flex justify-content-between align-items-center">
                <strong>Quản lý User</strong>
                <CButton color="primary" onClick={() => openAdd()}>
                    Thêm User
                </CButton>
            </CCardHeader>
            <CCardBody>
                <CTable>
                    <CTableHead>
                        <CTableRow>
                            <CTableHeaderCell scope="col">#</CTableHeaderCell>
                            <CTableHeaderCell scope="col">Class</CTableHeaderCell>
                            <CTableHeaderCell scope="col">Heading</CTableHeaderCell>
                            <CTableHeaderCell scope="col">Action</CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>
                        {users.map((item) => (
                            <CTableRow key={item.id}>

                                <CTableHeaderCell scope="row">{item.id}</CTableHeaderCell>
                                <CTableDataCell>{item.username}</CTableDataCell>
                                <CTableDataCell>{item.email}</CTableDataCell>

                                <CTableDataCell>
                                    <CButton size="sm"
                                        color="warning"
                                        className="me-2"
                                        onClick={() => openEdit(item)} >
                                        Sửa
                                    </CButton>
                                    <CButton size="sm"
                                        color="warning"
                                        className="me-2">
                                        Xóa
                                    </CButton>
                                </CTableDataCell>
                            </CTableRow>
                        ))}


                    </CTableBody>
                </CTable>
            </CCardBody>
        </CCard>
    )
}
