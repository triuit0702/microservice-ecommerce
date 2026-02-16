import React, { useState, useEffect } from 'react';
import { CButton, CModal, CModalHeader, CModalBody, CModalFooter, CFormLabel, CFormInput } from '@coreui/react';
import { uploadImage } from '../../services/ProductService';

import { CSpinner } from '@coreui/react'
import FullPageLoader from '../common/FullPageLoader';



const VariantModal = ({ visible, onClose, onSave, variantData }) => {
    const [variant, setVariant] = useState({
        sku: '',
        size: '',
        color: '',
        price: '',
        stockQuantity: '',
        imageUrl: '',
        imagePublicId: '',
        previewUrl: '',
        material: '',
    });

    const [uploading, setUploading] = useState(false);

    const [preview, setPreview] = useState(null);

    // const [variants, setVariants] = useState([]);

    useEffect(() => {
        if (variantData) {
            // console.log(variantData);
            setVariant(variantData);
            setPreview(variantData.imageUrl);
            // setVariant(prev => ({
            //     ...defaultVariant,
            //     ...variantData,
            // }));
        } else {
            setVariant({
                sku: '',
                size: '',
                color: '',
                price: '',
                stockQuantity: '',
                imageUrl: '',
                imagePublicId: '',
                previewUrl: '',
                material: '',
            })
        }
    }, [variantData]);

    const handleChange = (e) => {
        setVariant({ ...variant, [e.target.name]: e.target.value });
    };

    const handleSave = () => {
        onSave(variant);
        setVariant({
            sku: '',
            color: '',
            size: '',
            price: '',
            stockQuantity: '',
            imageUrl: '',
            imagePublicId: '',
            previewUrl: '',
            material: '',
        });
    };

    const handleUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setUploading(true);
        try {

            // preview
            const previewUrl = URL.createObjectURL(file);

            // upload
            const formData = new FormData();
            formData.append("file", file);

            setPreview(URL.createObjectURL(file));

            const res = await uploadImage(formData);

            console.log("res: ");
            console.log(res);
            console.log(res.data.url);
            console.log(res.data.publicId);

            setVariant(prev => ({
                ...prev,
                imageUrl: res.data.url,
                previewUrl: previewUrl,
                imagePublicId: res.data.publicId
            }));

        } catch (err) {
            alert("Upload failed");
        } finally {
            setUploading(false);
        }

    };



    return (
        <CModal visible={visible} onClose={onClose}>
            <CModalHeader>{variantData ? 'Edit Variant' : 'Add Variant'}</CModalHeader>
            <CModalBody>
                {/* SKU là mã định danh DUY NHẤT cho từng sản phẩm / từng variant trong kho */}
                <CFormLabel>SKU</CFormLabel>
                <CFormInput name="sku" value={variant.sku} onChange={handleChange} />

                <CFormLabel>Color</CFormLabel>
                <CFormInput name="color" value={variant.color} onChange={handleChange} />

                <CFormLabel>Size</CFormLabel>
                <CFormInput name="size" value={variant.size} onChange={handleChange} />

                <CFormLabel>Price</CFormLabel>
                <CFormInput type="number" name="price" value={variant.price} onChange={handleChange} />

                <CFormLabel>Stock</CFormLabel>
                <CFormInput type="number" name="stockQuantity" value={variant.stockQuantity} onChange={handleChange} />




                <CFormLabel>Material</CFormLabel>
                <CFormInput name="material" value={variant.material} onChange={handleChange} />

                <CFormLabel>Image URL</CFormLabel>
                <CFormInput type="file" name="imageUrl" onChange={(e) => handleUpload(e)} />
                {preview && (
                    <img
                        src={preview}
                        alt="preview"
                        style={{ width: 120, marginTop: 10 }}
                    />
                )}
            </CModalBody>
            <CModalFooter>
                <CButton color="secondary" onClick={onClose}>Cancel</CButton>
                <CButton color="primary" onClick={handleSave}>Save
                    {/* {uploading && <CSpinner size="sm" />} */}
                </CButton>
            </CModalFooter>

            {uploading && <FullPageLoader />}
        </CModal>
    );
};

export default VariantModal;
