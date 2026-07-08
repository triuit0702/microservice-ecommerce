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
    CFormCheck,
    CAlert
} from '@coreui/react';
import VariantModal from './VariantModal';
import { serviceGetAllCategories } from '../../services/CategoryService';
import { createProduct, getProductById, updateProduct } from '../../services/ProductService';
import { useNavigate, useParams } from 'react-router-dom'

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

import './product.css'

import {
    CRow,
    CCol
} from '@coreui/react'
import FormInput from '../common/FormInput';
import FormFileInput from '../common/FormFileInput ';
import FormCategoryCheckbox from '../common/CategorySelector';
import FullPageLoader from '../common/FullPageLoader';


// const schema = z.object({
//     email: z.string().email("Email không hợp lệ"),
//     password: z.string().min(6, "Mật khẩu tối thiểu 6 ký tự"),
//     username: z.string().min(3, "Username tối thiểu 3 ký tự"),
// });



const ProductForm = ({ visibleProductForm }) => {



    const {
        control,
        register,
        handleSubmit,
        setError,
        setFocus,
        clearErrors,
        setValue,
        getValues,
        watch,
        reset,
        formState: { errors },
    } = useForm(
        {
            defaultValues: {
                id: '',
                name: '',
                description: '',
                imageFile: '',
                imageName: '',
                imageUrl: '',
                price: '',
                discount: '',
                categoryIds: [],
                variants: [],
                version: 0
            },
            mode: "onChange",
            // reValidateMode: "onSubmit"
        }

    );

    const navigate = useNavigate()
    const [categories, setCategories] = useState([]);

    const [modalVisible, setModalVisible] = useState(false);
    const [editingVariantIndex, setEditingVariantIndex] = useState(null);

    const [imagePreview, setImagePreview] = useState(null);

    // error message 
    const [globalError, setGlobalError] = useState(null);
    const [variantErrors, setVariantErrors] = useState({})

    // use to display screen loading
    const [uploading, setUploading] = useState(false);

    // get productId when edit product
    const { id } = useParams();
    const isEdit = !!id

    const product = watch();

    console.log("product");
    console.log(product);


    const onSubmit = async (data) => {
        setUploading(true);
        clearErrors("variants");
        const formData = buildFormData(data);
        try {
            if (data.id) {
                await updateProduct(data.id, formData);
                // alert('Product updated successfully!');
            } else {
                await createProduct(formData);
                //alert('Product created successfully!');
            }

        } catch (error) {
            let resError = error.response.data.error;
            const newVariantErrors = {}
            // 🔥 1. Nếu backend trả lỗi theo field
            if (resError.details) {


                Object.entries(resError.details).forEach(([field, message]) => {

                    if (field.startsWith("variants[")) {

                        const match = field.match(/variants\[(\d+)\]\.(.+)/)

                        if (match) {
                            const index = parseInt(match[1])
                            const fieldName = match[2]

                            if (!newVariantErrors[index]) {
                                newVariantErrors[index] = {}
                            }

                            newVariantErrors[index][fieldName] = message
                        }

                    } else {
                        // lỗi của product
                        setError(field, {
                            type: "server",
                            message,
                        });
                    }
                })

                setVariantErrors(newVariantErrors)

                // focus field lỗi đầu tiên
                const firstField = Object.keys(resError.details)[0];
                setFocus(firstField);
            }

        } finally {
            setUploading(false);
            navigate('/products');
        }
    };

    useEffect(() => {


        serviceGetAllCategories().then((res) => {
            let data = res.data.data;
            setCategories(data);
        })

        if (id) {
            // get product detail when edit
            getProductById(id)
                .then(res => {
                    reset(res.data.data);
                })
                .catch(err => console.error(err));
        }

    }, [id, reset]);



    // add variant to list variant
    const handleSaveVariant = (variant) => {
        const updatedVariants = [...product.variants];
        if (editingVariantIndex !== null) {
            updatedVariants[editingVariantIndex] = variant;
            setEditingVariantIndex(null);
        } else {
            updatedVariants.push(variant);
        }
        setValue("variants", updatedVariants)
        setModalVisible(false);
        clearErrors("variants");
    };

    // open screen edit variant
    const handleEditVariant = (index) => {
        setEditingVariantIndex(index);
        setModalVisible(true);
    };

    // delete variant
    const handleDeleteVariant = (index) => {
        const variants = getValues("variants");
        variants.splice(index, 1);
        setValue("variants", variants);
        // TODO: delete image in cloudinary
    };


    // build form data
    const buildFormData = (obj) => {
        const formData = new FormData();

        Object.entries(obj).forEach(([key, value]) => {
            if (value === null || value === undefined) return;

            if (value instanceof FileList) {
                if (value.length > 0) {
                    formData.append(key, value[0]);
                }
                return;
            }

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

    // open variant modal
    const addVariant = () => {
        setEditingVariantIndex(null);
        setModalVisible(true);
    }


    return (
        <CCard visible={visibleProductForm}>

            <form onSubmit={handleSubmit(
                onSubmit,
                (errors) => {
                    console.log("VALIDATION FAIL:", errors);
                }
            )}>
                {/* ✅ Hiển thị lỗi ở đầu trang */}


                {globalError && (
                    <CAlert
                        color="danger"
                        dismissible
                        onClose={() => setFieldErrors({})}
                    >
                        <div >{globalError}</div>
                    </CAlert>
                )}


                <CCardHeader >
                    <h3>{isEdit ? 'Update Product' : 'Add Product'}</h3>
                </CCardHeader>
                <CCardBody>
                    {/* name */}
                    <FormInput
                        label="Name"
                        require={true}
                        name="name"
                        register={register}
                        errors={errors}
                    />
                    {/* description */}
                    <FormInput
                        label="Description"
                        name="description"
                        register={register}
                        errors={errors}
                    />
                    {/* image  */}
                    <FormFileInput
                        label="Image URL"
                        name="imageFile"
                        register={register}
                        errors={errors}
                        setValue={setValue}
                        watch={watch}
                    />
                    {/* price */}
                    <FormInput
                        label="Price"
                        name="price"
                        require={true}
                        register={register}
                        errors={errors}
                    />

                    {/* discount */}
                    <FormInput
                        label="Discount"
                        name="discount"
                        type="number"
                        register={register}
                        errors={errors}
                    />
                    {/* category */}
                    <FormCategoryCheckbox
                        label="Category"
                        name="categoryIds"
                        control={control}
                        categories={categories}
                        errors={errors}
                    />

                    <hr />
                    {/* variants */}
                    <input
                        className=""
                        type="hidden"
                        {...register("variants")}
                    />

                    {Object.keys(variantErrors).length > 0 && (
                        <div className="alert alert-danger">
                            Some variants have errors. Please check the highlighted rows.
                        </div>
                    )}
                    <h5>Product Variants <span className="text-danger">*</span></h5>

                    <CTable className={`${errors && errors.variants ? 'border border-danger' : ''}`}>
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
                            {product && product.variants.map((v, idx) => (

                                <CTableRow key={idx} className={variantErrors[idx] ? "table-danger" : ""}>
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


                    {errors && errors.variants && (
                        <div className="text-danger small">
                            {errors.variants.message}
                        </div>
                    )}

                    <CButton color="secondary" style={{ marginTop: '10px' }} onClick={() => addVariant()}>Add Variant</CButton>
                    <br />
                    <CButton color="primary" type="submit" style={{ marginTop: '20px' }}>Save</CButton>
                    {/* </CForm> */}

                </CCardBody>
            </form>

            {/* variant modal */}
            <VariantModal
                visible={modalVisible}
                onClose={() => setModalVisible(false)}
                serverErrors={variantErrors[editingVariantIndex]}
                onSave={handleSaveVariant}

                variantData={editingVariantIndex !== null ? product.variants[editingVariantIndex] : null}
            />
            {/* loading screen */}
            {uploading && <FullPageLoader />}
        </CCard>
    );
};

export default ProductForm;
