import { useState } from "react";
import { CFormLabel, CFormInput } from "@coreui/react";
import { useFormContext } from "react-hook-form";
import { useEffect } from "react";

const FormFileInput = ({
    label,
    name,
    register,
    setValue,
    watch,
    errors,
    rest
}) => {

    // const {
    //     setValue,
    //     watch,
    //     formState: { errors }
    // } = useFormContext();

    const imageUrl = watch("imageUrl");

    // init validate main image File
    useEffect(() => {
        register("imageUrl", {
            required: "Image is required"
        });

    }, [register]);



    const handleFileChange = (e) => {
        const file = e.target.files[0];

        if (file) {
            // tạo preview local
            const reader = new FileReader();
            reader.onloadend = () => {
                //setImageUrl(reader.result);
                setValue("imageUrl", reader.result)
            };
            reader.readAsDataURL(file);

            // gọi upload function nếu có
            // if (onUpload) {
            //     onUpload(file);
            // }
        }
    };

    return (
        <>
            <CFormLabel>{label}</CFormLabel>

            <CFormInput
                type="file"
                invalid={!!errors.imageUrl}
                {...register(name)}
                onChange={handleFileChange}
                {...rest}
            />

            {errors && errors.imageUrl && (
                <div className="text-danger small">
                    {errors.imageUrl.message}
                </div>
            )}

            {imageUrl && (
                <div style={{ marginTop: "10px" }}>
                    <img
                        src={imageUrl}
                        alt="Preview"
                        style={{
                            maxWidth: "200px",
                            maxHeight: "200px",
                            objectFit: "contain",
                            borderRadius: "8px",
                            border: "1px solid #ddd",
                        }}
                    />
                </div>
            )}
        </>
    );
};

export default FormFileInput;