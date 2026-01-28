import React, { useEffect, useState } from 'react';
import { CCard, CCardBody, CForm, CFormLabel, CFormInput, CFormTextarea, CButton, CTable, CTableHead, CTableRow, CTableHeaderCell, CTableBody, CTableDataCell, CCardHeader, CFormSelect } from '@coreui/react';
import VariantModal from './VariantModal';
import { serviceGetAllCategories } from '../../services/CategoryService';
import { createProduct } from '../../services/ProductService';
//import { createProduct } from '../services/productService';

const ProductForm = ({ visibleProductForm }) => {
    const [product, setProduct] = useState({
        name: '',
        description: '',
        imageFile: '',
        imageName: '',
        price: '',
        discount: '',
        categoryIds: [],
        variants: [],
    });

    const [categories, setCategories] = useState([]);

    const [modalVisible, setModalVisible] = useState(false);
    const [editingVariantIndex, setEditingVariantIndex] = useState(null);

    const [imagePreview, setImagePreview] = useState(null);


    useEffect(() => {


        serviceGetAllCategories().then((res) => {
            let data = res.data.data;
            setCategories(data);
        })
    }, []);



    const handleProductChange = (e) => {
        setProduct({ ...product, [e.target.name]: e.target.value });
    };

    const handleSaveVariant = (variant) => {
        console.log("truoc khi update variant");
        console.log(variant);
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
        // console.log(product);
        const formData = buildFormData(product);
        console.log(formData);
        try {
            await createProduct(formData);
            alert('Product created successfully!');
            setProduct({
                name: '',
                description: '',
                imageFile: '',
                imageName: '',

                price: '',
                discount: '',
                categoryIds: [],
                variants: [],
            });
        } catch (error) {
            console.error(error);
            alert('Error creating product');
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
            if (typeof value === "object" && (key == "imageUrl" || key == "previewUrl")) {
                console.log("key: " + key);
                return;
            }
            // array / object
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


    return (
        <CCard visible={visibleProductForm}>
            <CCardHeader >
                <h3>Add Product</h3>
            </CCardHeader>
            <CCardBody>

                <CForm onSubmit={handleSubmit}>
                    <CFormLabel>Name</CFormLabel>
                    <CFormInput name="name" value={product.name} onChange={handleProductChange} required />

                    <CFormLabel>Description</CFormLabel>
                    <CFormTextarea name="description" value={product.description} onChange={handleProductChange} />

                    <CFormLabel>Image URL</CFormLabel>
                    <CFormInput name="imageUrl" type="file" value={product.imageName} onChange={(e) => uploadMainImage(e)} />

                    {imagePreview && (
                        <div style={{ marginTop: "10px" }}>
                            <img
                                src={imagePreview}
                                alt="Preview"
                                style={{
                                    width: "150px",
                                    height: "150px",
                                    objectFit: "cover",
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

                    <CFormLabel>Category IDs (comma separated)</CFormLabel>
                    {/* <CFormInput
                        name="categoryIds"
                        value={product.categoryIds.join(',')}
                        onChange={(e) => setProduct({ ...product, categoryIds: e.target.value.split(',') })}
                    /> */}

                    <CFormSelect
                        id="product-category"
                        name="category"
                        value={product.category}
                        onChange={(e) => setProduct({ ...product, category: e.target.value })}
                    >
                        <option value="">Select Category</option>
                        {categories && categories.length > 0 && categories.map(cat => (
                            <option key={cat.id} value={cat.id}>
                                {cat.name}
                            </option>
                        ))}



                        {/* <option value="electronics">Electronics</option>
                        <option value="clothing">Clothing</option>
                        <option value="books">Books</option> */}
                    </CFormSelect>

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
                                    <CTableDataCell>{v.stock}</CTableDataCell>
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
                    <CButton color="primary" type="submit" style={{ marginTop: '20px' }}>Create Product</CButton>
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
