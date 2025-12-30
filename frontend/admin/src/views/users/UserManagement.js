

import { useEffect, useState } from 'react'
import {
    CButton,
    CCard,
    CCardBody,
    CCardHeader,
    CPagination,
    CPaginationItem,
    CTable,
    CTableBody,
    CTableDataCell,
    CTableHead,
    CTableHeaderCell,
    CTableRow,
} from '@coreui/react'
import ModalAddUser from './ModalUser'
import {
    serviceGetAllUser,
    serviceSaveUser,
    serviceDeleteUser,
    serviceUpdateUser
} from '../../services/UserService';
import Pagination from '../common/Pagination';
import ModalConfirm from '../common/ModalConfirm';

import { useToast } from '../../contexts/ToastContext'

export default function UserManagement() {
    // const [users, setUsers] = useState([
    //     { id: 1, name: 'admin', email: 'admin@gmail.com', description: 'aaa' },
    //     { id: 2, name: 'user1', email: 'user1@gmail.com', description: 'aaa' },
    // ])

    const { showToast } = useToast()

    const [visibleModal, setVisibleModal] = useState(false);
    const [editingUser, setEditingUser] = useState(false);

    const [users, setUsers] = useState([]);
    const [userDetail, setUserDetail] = useState({
        id: null,
        name: '',
        email: '',
        roleId: ''
    });

    // pagination
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const pageSize = 10;

    // delete
    const [visibleDelete, setVisibleDelete] = useState(false);
    const [deleteUserId, setDeleteUserId] = useState(null);


    const loadUsers = async () => {
        let res = await serviceGetAllUser(page, pageSize);
        setUsers(res.data.content);
        setTotalPages(res.data.totalPages);
    }

    useEffect(() => {
        loadUsers();
    }, [page])

    function openAdd() {
        setVisibleModal(true);
        setEditingUser(false);

        // console.log("visible: ", visibleModal); ham set state chay bat dong bo nên ko có in ra ket qua liền
    }

    function openEdit(item) {
        setVisibleModal(true);
        setEditingUser(true);
        setUserDetail(item);
    }

    const handleSave = async (data) => {
        console.log(data);

        if (data.id === null) {
            await serviceSaveUser(data);
            showToast('Tạo user mới thành công', 'success');

        } else {
            await serviceUpdateUser(data);
            showToast('Sửa user thành công', 'success');
        }

        setVisibleModal(false);

        loadUsers();

    }

    // delete user
    const confirmDelete = async (userId) => {
        setDeleteUserId(userId);
        setVisibleDelete(true);
    }

    const deleteUser = async () => {
        await serviceDeleteUser(deleteUserId);
        setVisibleDelete(false);
        showToast('Xóa user thành công', 'success');
        loadUsers();
    }

    return (
        <CCard>
            <ModalConfirm
                visibleDelete={visibleDelete}
                message="Bạn có chắc chắn muốn xóa user này không?"
                hideModalConfirm={() => {
                    setVisibleDelete(false);
                    setDeleteUserId(null)
                }}
                deleteUser={deleteUser}
            />
            <ModalAddUser
                visible={visibleModal}
                editingUser={editingUser}
                hideModal={() => setVisibleModal(false)}
                userDetail={userDetail}
                setUserDetail={setUserDetail}
                saveUser={handleSave}
            />

            <CCardHeader className="d-flex justify-content-between align-items-center">
                <strong>Quản lý User</strong>
                <CButton color="primary" onClick={() => openAdd()}>
                    Thêm User
                </CButton>
            </CCardHeader>

            {/* body table */}
            <CCardBody>
                <CTable>
                    <CTableHead>
                        <CTableRow>
                            <CTableHeaderCell scope="col">ID</CTableHeaderCell>
                            <CTableHeaderCell scope="col">User Name</CTableHeaderCell>
                            <CTableHeaderCell scope="col">Email</CTableHeaderCell>
                            <CTableHeaderCell scope="col">Action</CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>
                        {users && users.length > 0 && users.map(item => (
                            <CTableRow key={item.id}>

                                <CTableHeaderCell scope="row">{item.id}</CTableHeaderCell>
                                <CTableDataCell>{item.name}</CTableDataCell>
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
                                        className="me-2"
                                        onClick={() => confirmDelete(item.id)} >
                                        Xóa
                                    </CButton>
                                </CTableDataCell>
                            </CTableRow>
                        ))}


                    </CTableBody>
                </CTable>

                {/* Pagination */}
                <Pagination page={page}
                    totalPages={totalPages}
                    onPageChange={setPage} />



            </CCardBody>
        </CCard>
    )
}
