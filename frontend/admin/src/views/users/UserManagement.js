

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
    serviceUpdateUser,
    serviceGetRoles
} from '../../services/UserService';
import { USER_CREATE, USER_DELETE, USER_UPDATE, hasPermission } from '../../constants/permissions';
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
    const [userDetail, setUserDetail] = useState(null);
    const [roles, setRoles] = useState([]);

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
        const res = await serviceGetAllUser(page, pageSize);
        const pageData = res.data.data;
        setUsers(pageData.content);
        setTotalPages(pageData.totalPages);
    }

    useEffect(() => {
        loadUsers();
    }, [page])

    // role options for the form (code + label), no hard-coded role ids
    useEffect(() => {
        serviceGetRoles().then((res) => setRoles(res.data.data)).catch(() => setRoles([]));
    }, [])

    function openAdd() {
        setUserDetail(null)
        setVisibleModal(true);
        setEditingUser(false);
    }

    function openEdit(item) {
        setVisibleModal(true);
        setEditingUser(true);
        setUserDetail(item);
    }

    // roles are sent as an array (roleCodes) so the backend already supports several roles per user
    const handleSave = async (data, customerAccount) => {
        const roleCodes = data.roleCode ? [data.roleCode] : [];
        try {
            if (data.id === null) {
                await serviceSaveUser({ name: data.name, email: data.email, password: data.password, roleCodes });
                showToast('Tạo user mới thành công', 'success');
            } else {
                // null keeps the current roles (customer accounts cannot change role)
                await serviceUpdateUser(data.id, {
                    name: data.name,
                    email: data.email,
                    roleCodes: customerAccount ? null : roleCodes
                });
                showToast('Sửa user thành công', 'success');
            }
        } catch {
            // error toast is shown by AxiosClient
            return;
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
        try {
            await serviceDeleteUser(deleteUserId);
        } catch {
            setVisibleDelete(false);
            return;
        }
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
        if (hasPermission(userLogin, USER_UPDATE)) {
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
        // cannot delete yourself (also checked by the backend)
        if (hasPermission(userLogin, USER_DELETE) && item.id !== userLogin.id) {
            return <CButton size="sm"
                color="danger"
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
                roles={roles}
                saveUser={handleSave}
            />

            <CCardHeader className="d-flex justify-content-between align-items-center">
                <strong>Quản lý User</strong>
                {
                    hasPermission(userLogin, USER_CREATE) && (
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
                            <CTableHeaderCell scope="col">Roles</CTableHeaderCell>
                            <CTableHeaderCell scope="col">{tUser("email")}</CTableHeaderCell>
                            <CTableHeaderCell scope="col" className="text-center">{tCommon("action")}</CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>
                        {users && users.length > 0 ? (users.map(item => (
                            <CTableRow key={item.id}>

                                <CTableHeaderCell scope="row">{item.id}</CTableHeaderCell>

                                <CTableDataCell>{item.name}</CTableDataCell>
                                <CTableDataCell>{item.roles?.join(", ")}</CTableDataCell>
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
