import { CFormInput, CFormLabel } from "@coreui/react"

const FormInput = ({
    label,
    require,
    name,
    handleChange,
    type = "text",
    register,
    errors,
    // rules,
    ...rest
}) => {




    return (
        <>
            <CFormLabel>{label} {require && <span className="text-danger">*</span>}</CFormLabel>

            <CFormInput
                className=""
                type={type}
                invalid={!!errors[name]}
                {...register(name)}
                {...rest}
            />

            {errors[name] && (
                <div className="text-danger small mb-3">
                    {errors[name].message}
                </div>
            )}
        </>
    )
}

export default FormInput