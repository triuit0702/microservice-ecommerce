import { useEffect, useState } from 'react';
import {
    CCard,
    CCardBody,
    CForm,
    CFormLabel,
    CFormInput,
    CFormTextarea,
    CButton,
    CTable,
    CTableHead,
    CTableRow,
    CTableHeaderCell,
    CTableBody,
    CTableDataCell,
    CCardHeader,
    CFormSelect,
    CFormCheck
} from '@coreui/react';
import VariantModal from './VariantModal';
import { serviceGetAllCategories } from '../../services/CategoryService';
import { createProduct, getProductById, updateProduct } from '../../services/ProductService';
import { useParams } from 'react-router-dom'

import './product.css'

import {
    CRow,
    CCol
} from '@coreui/react'

const ProductForm = ({ visibleProductForm }) => {





    const [product, setProduct] = useState({
        id: '',
        name: '',
        description: '',
        imageFile: '',
        imageName: '',
        price: '',
        discount: '',
        categoryIds: [],
        variants: [],
        version: 0
    });

    const [categories, setCategories] = useState([]);

    const [modalVisible, setModalVisible] = useState(false);
    const [editingVariantIndex, setEditingVariantIndex] = useState(null);

    const [imagePreview, setImagePreview] = useState(null);

    // get productId when edit product
    const { id } = useParams();


    useEffect(() => {


        serviceGetAllCategories().then((res) => {
            let data = res.data.data;
            setCategories(data);
        })

        if (id) {
            // get product detail when edit
            getProductById(id)
                .then(res => {
                    console.log(res.data.data);
                    setProduct(res.data.data);
                    setImagePreview(res.data.data.imageUrl);
                })
                .catch(err => console.error(err));
        }


    }, []);



    const handleProductChange = (e) => {
        setProduct({ ...product, [e.target.name]: e.target.value });
    };

    const handleSaveVariant = (variant) => {
        const updatedVariants = [...product.variants];
        if (editingVariantIndex !== null) {
            updatedVariants[editingVariantIndex] = variant;
            setEditingVariantIndex(null);
        } else {
            updatedVariants.push(variant);
        }
        setProduct({ ...product, variants: updatedVariants });
        setModalVisible(false);

        console.log(updatedVariants);
    };

    const handleEditVariant = (index) => {
        setEditingVariantIndex(index);
        setModalVisible(true);
    };

    const handleDeleteVariant = (index) => {
        const updatedVariants = [...product.variants];
        updatedVariants.splice(index, 1);
        setProduct({ ...product, variants: updatedVariants });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const formData = buildFormData(product);
        try {
            if (product.id) {
                await updateProduct(product.id, formData);
                alert('Product updated successfully!');
            } else {
                await createProduct(formData);
                alert('Product created successfully!');
            }

            setProduct({
                id: '',
                name: '',
                description: '',
                imageFile: '',
                imageName: '',

                price: '',
                discount: '',
                categoryIds: [],
                variants: [],
                version: 0
            });
        } catch (error) {
            console.error(error);
            alert('Error save  product');
        }
    };

    const buildFormData = (obj) => {
        const formData = new FormData();

        console.log(obj);
        Object.entries(obj).forEach(([key, value]) => {
            if (value === null || value === undefined) return;

            // file
            if (value instanceof File) {
                formData.append(key, value);
                return;
            }

            if (key === "variants") {
                formData.append(key, JSON.stringify(value));
                return;
            }
            // array 
            if (Array.isArray(value)) {
                value.forEach(v => {
                    formData.append(key, v);
                });
                return;
            }
            if (typeof value === "object" && (key == "imageUrl" || key == "previewUrl")) {
                console.log("key: " + key);
                return;
            }
            //  object
            if (typeof value === "object") {
                formData.append(key, JSON.stringify(value));
                return;
            }

            // primitive
            formData.append(key, value);
        });

        return formData;
    };

    const addVariant = () => {
        setEditingVariantIndex(null);
        setModalVisible(true);

    }

    const uploadMainImage = (e) => {
        const { name, files } = e.target;
        const value = e.target.value;

        if (name === "imageUrl" && files && files[0]) {
            const file = files[0];


            setProduct({
                ...product,
                imageName: value,
                imageFile: file, // lưu file nếu cần submit
            });

            // setProduct({ ...product, [e.target.name]: e.target.value });

            // // tạo preview
            setImagePreview(URL.createObjectURL(file));
        }
    };

    const toggleCategory = (id) => {
        setProduct(prev => ({
            ...prev,
            categoryIds: prev.categoryIds.includes(id)
                ? prev.categoryIds.filter(c => c !== id) // bỏ chọn nếu nó đang chọn
                : [...prev.categoryIds, id] //  (thêm vô mảng )chọn nếu hiện tại chưa đuoc chọn
        }))
    }


    return (
        <CCard visible={visibleProductForm}>
            <CCardHeader >
                <h3>{product.id ? 'Update Product' : 'Add Product'}</h3>
            </CCardHeader>
            <CCardBody>

                <CForm onSubmit={handleSubmit}>
                    <CFormLabel>Name</CFormLabel>
                    <CFormInput name="name" value={product.name} onChange={handleProductChange} required />

                    <CFormLabel>Description</CFormLabel>
                    <CFormTextarea name="description" value={product.description} onChange={handleProductChange} />

                    <CFormLabel>Image URL</CFormLabel>
                    <CFormInput name="imageUrl" type="file" onChange={(e) => uploadMainImage(e)} />

                    {imagePreview && (
                        <div style={{ marginTop: "10px" }}>
                            <img
                                src={imagePreview}
                                alt="Preview"
                                style={{
                                    maxWidth: "200px",
                                    maxHeight: "200px",
                                    width: "auto",
                                    height: "auto",
                                    objectFit: "contain",
                                    borderRadius: "8px",
                                    border: "1px solid #ddd",
                                }}
                            />
                        </div>
                    )}






                    <CFormLabel>Price</CFormLabel>
                    <CFormInput type="number" name="price" value={product.price} onChange={handleProductChange} required />

                    <CFormLabel>Discount</CFormLabel>
                    <CFormInput type="number" name="discount" value={product.discount} onChange={handleProductChange} />





                    <div className="mt-4">
                        <CFormLabel className="fw-semibold">
                            Category <span className="text-danger">*</span>
                        </CFormLabel>

                        <CCard className="mt-2">
                            <CCardBody className="category-scroll">
                                <CRow>
                                    {categories.map(c => (
                                        <CCol md={6} key={c.id}>
                                            <CFormCheck
                                                label={c.name}
                                                value={c.id}
                                                checked={product.categoryIds.includes(c.id)}
                                                onChange={() => toggleCategory(c.id)}
                                                className="mb-2"
                                            />
                                        </CCol>
                                    ))}
                                </CRow>
                            </CCardBody>
                        </CCard>
                    </div>

                    <hr />
                    <h5>Product Variants</h5>
                    <CTable>
                        <CTableHead>
                            <CTableRow>
                                <CTableHeaderCell>SKU</CTableHeaderCell>
                                <CTableHeaderCell>Color</CTableHeaderCell>
                                <CTableHeaderCell>Size</CTableHeaderCell>
                                <CTableHeaderCell>Price</CTableHeaderCell>
                                <CTableHeaderCell>Stock</CTableHeaderCell>
                                <CTableHeaderCell>Actions</CTableHeaderCell>
                            </CTableRow>
                        </CTableHead>
                        <CTableBody>
                            {product.variants.map((v, idx) => (
                                <CTableRow key={idx}>
                                    <CTableDataCell>{v.sku}</CTableDataCell>
                                    <CTableDataCell>{v.color}</CTableDataCell>
                                    <CTableDataCell>{v.size}</CTableDataCell>
                                    <CTableDataCell>{v.price}</CTableDataCell>
                                    <CTableDataCell>{v.stockQuantity}</CTableDataCell>
                                    <CTableDataCell>
                                        <CButton size="sm" color="info" onClick={() => handleEditVariant(idx)}>Edit</CButton>{' '}
                                        <CButton size="sm" color="danger" onClick={() => handleDeleteVariant(idx)}>Delete</CButton>
                                    </CTableDataCell>
                                </CTableRow>
                            ))}
                        </CTableBody>
                    </CTable>

                    <CButton color="secondary" style={{ marginTop: '10px' }} onClick={() => addVariant()}>Add Variant</CButton>
                    <br />
                    <CButton color="primary" type="submit" style={{ marginTop: '20px' }}>Save</CButton>
                </CForm>

                <VariantModal
                    visible={modalVisible}
                    onClose={() => setModalVisible(false)}
                    onSave={handleSaveVariant}
                    variantData={editingVariantIndex !== null ? product.variants[editingVariantIndex] : null}
                />
            </CCardBody>
        </CCard>
    );
};

export default ProductForm;
