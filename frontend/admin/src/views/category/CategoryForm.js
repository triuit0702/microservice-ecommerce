import React, { useEffect, useState } from 'react'
import {
    CCard,
    CCardBody,
    CCardHeader,
    CForm,
    CFormInput,
    CFormLabel,
    CFormSelect,
    CButton,
    CRow,
} from '@coreui/react'
import { useNavigate, useParams } from 'react-router-dom'
import {
    serviceGetById,
    serviceGetAllCategories,
    serviceCreateCategory,
    serviceUpdateCategory
} from '../../services/CategoryService'

const CategoryForm = () => {
    const navigate = useNavigate()

    const [categories, setCategories] = useState([])
    const [form, setForm] = useState({
        name: '',
        parentId: '',
    })
    const [errors, setErrors] = useState({})

    const { id } = useParams();

    //    Load category list for parent select
    useEffect(() => {
        serviceGetAllCategories()
            .then(res => {
                setCategories(res.data.data)
            })
            .catch(err => console.error(err));

        if (id) {
            serviceGetById(id)
                .then(res => {
                    console.log(res.data.data);
                    setForm(res.data.data);
                })
                .catch(err => console.error(err));
        }


    }, [])

    // handle change input form
    const handleChange = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value,
        })
    }

    const validate = () => {
        const err = {}
        if (!form.name.trim()) {
            err.name = 'Category name is required'
        }
        setErrors(err)
        return Object.keys(err).length === 0
    }

    // submit form
    const handleSubmit = (e) => {
        e.preventDefault()
        if (!validate()) return

        const payload = {
            name: form.name,
            parentId: form.parentId || null,
        }

        const action = form.id ? serviceUpdateCategory(form.id, payload) : serviceCreateCategory(payload);
        action.then(() => {
            navigate('/categories');
        })
    }

    return (

        <CCard>
            <CCardHeader>
                <strong>Create Category</strong>
            </CCardHeader>
            <CCardBody>
                <CForm onSubmit={handleSubmit}>
                    {/* Category Name */}
                    <div className="mb-3">
                        <CFormLabel>Name</CFormLabel>
                        <CFormInput
                            name="name"
                            placeholder="Enter category name"
                            value={form.name}
                            onChange={handleChange}
                            invalid={!!errors.name}
                        />
                        {errors.name && (
                            <div className="text-danger small">{errors.name}</div>
                        )}
                    </div>

                    {/* Parent Category */}
                    <div className="mb-3">
                        <CFormLabel>Parent Category</CFormLabel>
                        <CFormSelect
                            name="parentId"
                            value={form.parentId}
                            onChange={handleChange}
                        >
                            <option value="">-- No Parent --</option>
                            {categories && categories.length > 0 && categories.map(cat => (
                                <option key={cat.id} value={cat.id}>
                                    {cat.name}
                                </option>
                            ))}
                        </CFormSelect>
                    </div>

                    <div className="d-flex gap-2">
                        <CButton color="primary" type="submit">
                            {form.id ? "Update" : "Create"}
                        </CButton>
                        <CButton
                            color="secondary"
                            variant="outline"
                            onClick={() => navigate('/categories')}
                        >
                            Cancel
                        </CButton>
                    </div>
                </CForm>
            </CCardBody>
        </CCard>

    )
}

export default CategoryForm
