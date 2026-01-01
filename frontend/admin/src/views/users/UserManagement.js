

import { useEffect, useState } from 'react'
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
import {
    serviceGetAllUser,
    serviceSaveUser,
    serviceDeleteUser,
    serviceUpdateUser
} from '../../services/UserService';
import Pagination from '../common/Pagination';
import ModalConfirm from '../common/ModalConfirm';

import { useToast } from '../../contexts/ToastContext'

import { useTranslation } from "react-i18next";

import { useSelector } from "react-redux";

import { useSearchParams } from "react-router-dom";

export default function UserManagement() {

    // use redux get auth user
    const userLogin = useSelector(state => state.auth.user);

    // language
    // namespace "common"
    const { t: tCommon } = useTranslation("common");

    // namespace "user"
    const { t: tUser } = useTranslation("user");


    // toast
    const { showToast } = useToast()

    const [visibleModal, setVisibleModal] = useState(false);
    const [editingUser, setEditingUser] = useState(false);

    const [users, setUsers] = useState([]);
    const [userDetail, setUserDetail] = useState({
        id: null,
        name: '',
        email: '',
        roleId: '',
        permissions: ''
    });

    const [searchParams, setSearchParams] = useSearchParams();
    // Lấy page từ URL
    const page = parseInt(searchParams.get("page") || "0", 10);


    // pagination
    const [totalPages, setTotalPages] = useState(0);
    const pageSize = 10;

    // delete
    const [visibleDelete, setVisibleDelete] = useState(false);
    const [deleteUserId, setDeleteUserId] = useState(null);


    const loadUsers = async () => {
        let res = await serviceGetAllUser(page, pageSize);
        const usersWithPermission = res.data.content.map(user => ({
            ...user,
            permissions: user.roles?.map(r => r.authority).join(", ")
        }));

        setUsers(usersWithPermission);

        setTotalPages(res.data.totalPages);
    }

    useEffect(() => {
        loadUsers();
    }, [page])

    function openAdd() {
        setVisibleModal(true);
        setEditingUser(false);
    }

    function openEdit(item) {
        setVisibleModal(true);
        setEditingUser(true);
        setUserDetail(item);
    }

    const handleSave = async (data) => {
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
        showToast(tUser("deleteSuccess"), 'success');
        loadUsers();
    }

    // Hàm đổi page
    const handlePageChange = (newPage) => {
        setSearchParams({ page: newPage });
    };

    //  Edit button
    function EditButton({ item }) {
        let permissions = userLogin.permissions;
        let canSelfEdit = permissions && permissions.includes("SELF_USER_EDIT") && item.id == userLogin.id
        if (canSelfEdit || permissions && permissions.includes("USER_EDIT")) {
            return <CButton size="sm"
                color="warning"
                className="me-2"
                onClick={() => openEdit(item)} >
                Sửa
            </CButton>
        }
        return null;
    }

    // Delete button
    function DeleteButton({ item }) {
        if (userLogin.permissions && userLogin.permissions.includes("USER_DELETE")) {
            return <CButton size="sm"
                color="warning"
                className="me-2"
                onClick={() => confirmDelete(item.id)} >
                Xóa
            </CButton>
        }
        return null;
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
                {
                    userLogin && userLogin.permissions.includes("USER_CREATE") && (
                        <CButton color="primary" onClick={() => openAdd()}>
                            Thêm User
                        </CButton>
                    )
                }

            </CCardHeader>

            {/* body table */}
            <CCardBody>
                <CTable>
                    <CTableHead>
                        <CTableRow>
                            <CTableHeaderCell scope="col">{tUser("id")}</CTableHeaderCell>

                            <CTableHeaderCell scope="col">{tUser("name")}</CTableHeaderCell>
                            <CTableHeaderCell scope="col">Permission</CTableHeaderCell>
                            <CTableHeaderCell scope="col">{tUser("email")}</CTableHeaderCell>
                            <CTableHeaderCell scope="col" className="text-center">{tCommon("action")}</CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>
                        {users && users.length > 0 ? (users.map(item => (
                            <CTableRow key={item.id}>

                                <CTableHeaderCell scope="row">{item.id}</CTableHeaderCell>

                                <CTableDataCell>{item.name}</CTableDataCell>
                                <CTableDataCell>{item.permissions}</CTableDataCell>
                                <CTableDataCell>{item.email}</CTableDataCell>

                                <CTableDataCell >
                                    <div className="d-flex justify-content-center align-items-center">
                                        <EditButton item={item} />
                                        <DeleteButton item={item} />
                                    </div>
                                </CTableDataCell>
                            </CTableRow>
                        ))) : (
                            <CTableRow>
                                <CTableDataCell colSpan={4} className="text-center">
                                    Không có sản phẩm
                                </CTableDataCell>
                            </CTableRow>
                        )
                        }


                    </CTableBody>
                </CTable>

                {/* Pagination */}
                <Pagination page={page}
                    totalPages={totalPages}
                    onPageChange={handlePageChange} />



            </CCardBody>
        </CCard>
    )
}
