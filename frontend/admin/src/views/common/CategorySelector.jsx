import React from 'react'
import {
    CFormLabel,
    CCard,
    CCardBody,
    CRow,
    CCol,
    CFormCheck,
} from '@coreui/react'
import { Controller } from 'react-hook-form'

const FormCategoryCheckbox = ({
    label = 'Category',
    name,
    control,
    categories = [],
    errors,
    rules,
}) => {
    return (
        <>
            <CFormLabel className="fw-semibold">
                {label} <span className="text-danger">*</span>
            </CFormLabel>

            <Controller
                name={name}
                control={control}
                rules={rules}
                render={({ field }) => (
                    <CCard
                        className={`mt-2 ${errors?.[name] ? 'border border-danger' : ''
                            }`}
                    >
                        <CCardBody className="category-scroll">
                            <CRow>
                                {categories.map((c) => {
                                    const checked = field.value?.includes(c.id)

                                    const handleChange = () => {
                                        if (checked) {
                                            field.onChange(
                                                field.value.filter((id) => id !== c.id)
                                            )
                                        } else {
                                            field.onChange([...(field.value || []), c.id])
                                        }
                                    }

                                    return (
                                        <CCol md={6} key={c.id}>
                                            <CFormCheck
                                                label={c.name}
                                                checked={checked}
                                                onChange={handleChange}
                                                className="mb-2"
                                            />
                                        </CCol>
                                    )
                                })}
                            </CRow>
                        </CCardBody>
                    </CCard>
                )}
            />

            {errors?.[name] && (
                <div className="text-danger mt-1">
                    {errors[name].message}
                </div>
            )}
        </>
    )
}

export default FormCategoryCheckbox