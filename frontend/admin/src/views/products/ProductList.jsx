import React, { useEffect, useState } from "react";
import {
    CButton,
    CCard,
    CCardBody,
    CCardHeader,
    CTable,
    CTableHead,
    CTableRow,
    CTableHeaderCell,
    CTableBody,
    CTableDataCell,

} from "@coreui/react";
import { getProducts, createProduct, updateProduct, deleteProduct } from "../../services/ProductService";
import { useSelector } from "react-redux";
import { useNavigate } from 'react-router-dom';
import Pagination from "../common/Pagination";
import { useSearchParams } from "react-router-dom";

const ProductList = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const navigate = useNavigate();

    // page
    const [searchParams, setSearchParams] = useSearchParams();
    // Lấy page từ URL
    const page = parseInt(searchParams.get("page") || "0", 10);


    // pagination
    const [totalPages, setTotalPages] = useState(0);
    const pageSize = 10;

    // use redux get auth user
    const userLogin = useSelector(state => state.auth.user);
    let permissions = userLogin.permissions;

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const res = await getProducts(page, pageSize);
            setProducts(res.data.data.content);
            setTotalPages(res.data.data.totalPages);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProducts();
    }, [page]);

    const openAddModal = () => {
        setModalVisible(true);
    };

    // open edit modal
    const openEditModal = (product) => {

        navigate(`/products/edit/${product.id}`)
    };

    // delete product
    const handleDelete = async (id) => {
        if (window.confirm("Bạn có chắc muốn xóa sản phẩm này?")) {
            await deleteProduct(id);
            fetchProducts();
        }
    };

    // Hàm đổi page
    const handlePageChange = (newPage) => {
        setSearchParams({ page: newPage });
    };

    // submit create or update product
    const handleSubmit = async () => {
        try {
            if (editingProduct) {
                await updateProduct(editingProduct.id, formData);
            } else {
                await createProduct(formData);
            }
            setModalVisible(false);
            fetchProducts();
        } catch (err) {
            console.error(err);
        }
    };

    return (
        <CCard>


            <CCardHeader className="d-flex justify-content-between align-items-center">
                <h3>Products</h3>
                {/* {permissions.includes("CREATE_PRODUCT") && (
                    <CButton color="primary" onClick={openAddModal}>Add Product</CButton>
                )} */}
                <CButton color="primary" onClick={() => navigate('/products/add')}>Add Product</CButton>

            </CCardHeader>
            <CCardBody>
                <CTable striped hover responsive>
                    <CTableHead>
                        <CTableRow>
                            <CTableHeaderCell>ID</CTableHeaderCell>
                            <CTableHeaderCell>Name</CTableHeaderCell>
                            <CTableHeaderCell>Price</CTableHeaderCell>
                            <CTableHeaderCell>Actions</CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>
                        {products && products.length > 0 ? (products.map((product) => (
                            <CTableRow key={product.id}>
                                <CTableDataCell>{product.id}</CTableDataCell>
                                <CTableDataCell>{product.name}</CTableDataCell>
                                <CTableDataCell>{product.price}</CTableDataCell>
                                <CTableDataCell>
                                    <CButton className="me-2" size="sm" color="warning" onClick={() => openEditModal(product)}>Edit</CButton>
                                    <CButton size="sm" color="danger" onClick={() => handleDelete(product.id)}>Delete</CButton>
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



            </CCardBody>
            {/* Pagination */}
            <Pagination page={page}
                totalPages={totalPages}
                onPageChange={handlePageChange} />

        </CCard>
    );
};

export default ProductList;
