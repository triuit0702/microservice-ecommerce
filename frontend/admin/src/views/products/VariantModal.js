import React, { useState, useEffect } from 'react';
import { CButton, CModal, CModalHeader, CModalBody, CModalFooter, CFormLabel, CFormInput, CForm } from '@coreui/react';
import { uploadImage } from '../../services/ProductService';

import { CSpinner } from '@coreui/react'
import FullPageLoader from '../common/FullPageLoader';

import { useForm } from "react-hook-form"
import FormInput from '../common/FormInput';
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import FormFileUpload from '../common/FormFileUpload';



const schema = z.object({
    sku: z.string().min(1, "Sku is required"),
    color: z.string().min(1, "Color is required"),
    size: z.string().min(1, "Size is required"),
    price: z.coerce
        .string()
        .min(1, "Price is required")
        .regex(/^\d+(\.\d{1,2})?$/, "Price must be a valid decimal (max 2 decimals)"),
    stockQuantity: z.coerce
        .string()
        .min(1, "Stock is required")
        .regex(/^\d+(\.\d{1,2})?$/, "Stock must be a valid decimal (max 2 decimals)"),
    imageUrl: z.string().min(1, "Image is required"),
    imagePublicId: z.string().optional(),
    previewUrl: z.string().optional()
});

const VariantModal = ({ visible, onClose, serverErrors, onSave, variantData }) => {


    const { register, handleSubmit, setValue, setError, reset, formState: { errors } } = useForm({
        defaultValues: {
            sku: '',
            size: '',
            color: '',
            price: '',
            stockQuantity: '',
            imageUrl: '',
            imagePublicId: '',
            previewUrl: '',
            material: '',
        },
        resolver: zodResolver(schema),
        mode: "onSubmit",
    })

    const [uploading, setUploading] = useState(false);

    const [preview, setPreview] = useState(null);


    const emptyVariant = {
        sku: '',
        size: '',
        color: '',
        price: '',
        stockQuantity: '',
        imageUrl: '',
        imagePublicId: '',
        previewUrl: '',
        material: '',
    }

    // init data
    useEffect(() => {
        if (!visible) return

        if (variantData) {
            console.log("vairant data init: " + variantData);
            console.log(variantData);
            // Edit mode
            reset(variantData)
            setValue("previewUrl", variantData.imageUrl);

        } else {
            // Add mode
            reset(emptyVariant)
        }

    }, [visible, variantData, reset])


    // dùng hiển thị lỗi từ backend
    useEffect(() => {
        if (!visible) return
        if (!serverErrors) return

        Object.entries(serverErrors).forEach(([field, message]) => {
            setError(field, {
                type: "server",
                message,
            })
        })

    }, [visible, serverErrors, setError])



    // handle save
    const handleSave = (data) => {
        console.log("add modal save")
        console.log(data);
        try {
            onSave(data);
        } catch (err) {
            console.log(err);
        }

    };




    return (
        <CModal visible={visible} onClose={onClose} backdrop="static">
            <CForm onSubmit={handleSubmit(handleSave)}>
                <CModalHeader>{variantData ? 'Edit Variant' : 'Add Variant'}</CModalHeader>
                <CModalBody>
                    {/* SKU là mã định danh DUY NHẤT cho từng sản phẩm / từng variant trong kho */}
                    <FormInput
                        label="Sku"
                        name="sku"
                        register={register}
                        errors={errors}
                    />


                    <FormInput
                        label="Color"
                        name="color"
                        register={register}
                        errors={errors}
                    />


                    <FormInput
                        label="Size"
                        name="size"
                        register={register}
                        errors={errors}
                    />

                    <FormInput
                        label="Price"
                        name="price"
                        type="number"
                        register={register}
                        errors={errors}
                    />

                    <FormInput
                        label="Stock"
                        name="stockQuantity"
                        type="number"
                        register={register}
                        errors={errors}
                    />

                    <FormInput
                        label="Material"
                        name="material"
                        register={register}
                        errors={errors}
                    />
                    {/* <input type="hidden" {...register("imagePublicId")} /> */}

                    <input
                        type="hidden"
                        {...register("imagePublicId", {
                        })}
                    />

                    <input
                        type="hidden"
                        {...register("imageUrl", {
                            // required: "Image is required"
                        })}
                    />
                    <FormFileUpload
                        label="Image URL"
                        //name="imageUrl"
                        //register={register}
                        errors={errors}
                        setUploading={setUploading}
                        preview={preview}
                        setPreview={setPreview}
                        setValue={setValue}
                    />



                </CModalBody>
                <CModalFooter>
                    <CButton color="secondary" onClick={onClose}>Cancel</CButton>
                    <CButton type="submit" color="primary">Save
                        {/* {uploading && <CSpinner size="sm" />} */}
                    </CButton>
                </CModalFooter>
            </CForm>
            {uploading && <FullPageLoader />}

        </CModal >
    );
};

export default VariantModal;
