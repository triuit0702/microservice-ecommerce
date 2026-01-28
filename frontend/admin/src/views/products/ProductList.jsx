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

} from "@coreui/react";
import { getProducts, createProduct, updateProduct, deleteProduct } from "../../services/ProductService";
import { useSelector } from "react-redux";
import { useNavigate } from 'react-router-dom';

const ProductList = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [modalVisible, setModalVisible] = useState(false);
    //const [editingProduct, setEditingProduct] = useState(null);
    const navigate = useNavigate();


    // use redux get auth user
    const userLogin = useSelector(state => state.auth.user);
    let permissions = userLogin.permissions;

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const res = await getProducts();
            setProducts(res.data.data.content);
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
        setModalVisible(true);
    };

    const openEditModal = (product) => {

        // setEditingProduct(product);
        // setFormData({ name: product.name, price: product.price });
        // setModalVisible(true);

        navigate(`/products/edit/${product.id}`)
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
            {/* <ProductForm
                visible={modalVisible}
                editingProduct={false}
                hideModal={() => setModalVisible(false)}
            /> */}

            {/* <AddProduct
                visibleProductForm={false}
                editingProduct={false}
                hideModal={() => setModalVisible(false)}
            /> */}


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






        </CCard>
    );
};

export default ProductList;
