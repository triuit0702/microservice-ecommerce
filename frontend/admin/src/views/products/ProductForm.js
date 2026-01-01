import React, { useState } from 'react'
import {
    CCard,
    CCardBody,
    CCardHeader,
    CForm,
    CFormLabel,
    CFormInput,
    CFormTextarea,
    CButton,
    CModal,
    CModalHeader,
    CModalTitle,
    CModalBody,
    CModalFooter,
    CFormSelect,
} from '@coreui/react'

const ProductForm = ({ visible, editingProduct }) => {
    const [name, setName] = useState('')
    const [price, setPrice] = useState('')
    const [description, setDescription] = useState('')

    const [formData, setFormData] = useState({ name: "", price: "" });


    const onClose = () => {


    }


    const handleSubmit = (e) => {
        e.preventDefault()

        // Kiểm tra dữ liệu cơ bản
        if (!name || !price) {
            alert('Vui lòng nhập tên và giá sản phẩm')
            return
        }

        const newProduct = {
            name,
            price: parseFloat(price),
            description,
        }

        // Gửi dữ liệu lên parent hoặc API
        onSubmit(newProduct)

        // Reset form
        setName('')
        setPrice('')
        setDescription('')
    }

    return (
        <CModal visible={visible} onClose={() => setModalVisible(false)} size="lg">
            <CModalHeader>
                <CModalTitle>{editingProduct ? "Edit Product" : "Add Product"}</CModalTitle>
            </CModalHeader>




            <CModalBody style={{ maxHeight: '70vh', overflowY: 'auto', paddingRight: '10px' }}>
                {/* Product Name */}
                <div className="mb-3">
                    <label htmlFor="product-name" className="form-label">Product Name</label>
                    <CFormInput
                        id="product-name"
                        placeholder="Enter product name"
                        value={formData.name}
                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    />
                </div>

                {/* Price */}
                <div className="mb-3">
                    <label htmlFor="product-price" className="form-label">Price</label>
                    <CFormInput
                        id="product-price"
                        type="number"
                        placeholder="Enter price"
                        value={formData.price}
                        onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                    />
                </div>

                {/* Description */}
                <div className="mb-3">
                    <label htmlFor="product-description" className="form-label">Description</label>
                    <CFormTextarea
                        id="product-description"
                        placeholder="Enter description"
                        value={formData.description}
                        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    />
                </div>

                {/* Category */}
                <div className="mb-3">
                    <label htmlFor="product-category" className="form-label">Category</label>
                    <CFormSelect
                        id="product-category"
                        value={formData.category}
                        onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    >
                        <option value="">Select Category</option>
                        <option value="electronics">Electronics</option>
                        <option value="clothing">Clothing</option>
                        <option value="books">Books</option>
                    </CFormSelect>
                </div>

                {/* Stock Quantity */}
                <div className="mb-3">
                    <label htmlFor="product-stock" className="form-label">Stock Quantity</label>
                    <CFormInput
                        id="product-stock"
                        type="number"
                        placeholder="Enter stock quantity"
                        value={formData.stock}
                        onChange={(e) => setFormData({ ...formData, stock: e.target.value })}
                    />
                </div>

                {/* Image Upload */}
                <div className="mb-3">
                    <label htmlFor="product-image" className="form-label">Product Image</label>
                    <CFormInput
                        type="file"
                        id="product-image"
                        onChange={(e) => {
                            const file = e.target.files[0];
                            setFormData({ ...formData, image: file });

                            if (file) {
                                const reader = new FileReader();
                                reader.onload = (ev) => setFormData(prev => ({ ...prev, imagePreview: ev.target.result }));
                                reader.readAsDataURL(file);
                            }
                        }}
                    />
                    {formData.imagePreview && (
                        <img
                            src={formData.imagePreview}
                            alt="Preview"
                            style={{ marginTop: '10px', maxWidth: '100%', borderRadius: '6px' }}
                        />
                    )}
                </div>

            </CModalBody>
            <CModalFooter>
                <CButton color="secondary" onClick={() => setModalVisible(false)}>Cancel</CButton>
                <CButton color="primary" onClick={handleSubmit}>{editingProduct ? "Update" : "Add"}</CButton>
            </CModalFooter>
        </CModal>






    )
}

export default ProductForm
