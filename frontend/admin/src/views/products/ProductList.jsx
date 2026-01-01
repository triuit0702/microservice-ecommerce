// src/views/products/ProductList.jsx
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
    CModal,
    CModalHeader,
    CModalTitle,
    CModalBody,
    CModalFooter,
    CFormInput,
} from "@coreui/react";
import { getProducts, createProduct, updateProduct, deleteProduct } from "../../services/ProductService";
import { useSelector } from "react-redux";
import ProductForm from "./ProductForm";

const ProductList = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    const [editingProduct, setEditingProduct] = useState(null);
    const [formData, setFormData] = useState({ name: "", price: "" });


    // use redux get auth user
    const userLogin = useSelector(state => state.auth.user);
    let permissions = userLogin.permissions;

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const res = await getProducts();
            setProducts(res.data.data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProducts();
    }, []);

    const openAddModal = () => {
        setEditingProduct(null);
        setFormData({ name: "", price: "" });
        setModalVisible(true);
    };

    const openEditModal = (product) => {
        setEditingProduct(product);
        setFormData({ name: product.name, price: product.price });
        setModalVisible(true);
    };

    const handleDelete = async (id) => {
        if (window.confirm("Bạn có chắc muốn xóa sản phẩm này?")) {
            await deleteProduct(id);
            fetchProducts();
        }
    };

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
                <CButton color="primary" onClick={openAddModal}>Add Product</CButton>

            </CCardHeader>
            <CCardBody>
                <CTable striped hover responsive>
                    <CTableHead>
                        <CTableRow>
                            <CTableHeaderCell>ID</CTableHeaderCell>
                            <CTableHeaderCell>Name</CTableHeaderCell>
                            <CTableHeaderCell>Price</CTableHeaderCell>
                            {(permissions.includes("EDIT_PRODUCT") || permissions.includes("DELETE_PRODUCT")) && (
                                <CTableHeaderCell>Actions</CTableHeaderCell>
                            )}
                        </CTableRow>
                    </CTableHead>
                    <CTableBody>
                        {products && products.length > 0 ? (products.map((product) => (
                            <CTableRow key={product.id}>
                                <CTableDataCell>{product.id}</CTableDataCell>
                                <CTableDataCell>{product.name}</CTableDataCell>
                                <CTableDataCell>{product.price}</CTableDataCell>
                                {(permissions.includes("EDIT_PRODUCT") || permissions.includes("DELETE_PRODUCT")) && (
                                    <CTableDataCell>
                                        {permissions.includes("EDIT_PRODUCT") && (
                                            <CButton size="sm" color="warning" onClick={() => openEditModal(product)}>Edit</CButton>
                                        )}
                                        {permissions.includes("DELETE_PRODUCT") && (
                                            <CButton size="sm" color="danger" onClick={() => handleDelete(product.id)}>Delete</CButton>
                                        )}
                                    </CTableDataCell>
                                )}
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



            <ProductForm
                visible={modalVisible}
                editingProduct={editingProduct}
            />


        </CCard>
    );
};

export default ProductList;
