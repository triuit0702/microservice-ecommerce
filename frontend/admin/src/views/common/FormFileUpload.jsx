import { useState } from "react";
import { CFormLabel, CFormInput } from "@coreui/react";
import { uploadImage } from "../../services/ProductService";

const FormFileUpload = ({
    label,
    name,
    register,
    errors,
    onUpload,
    preview,
    setPreview,
    getValues,
    setUploading,
    watch,
    setValue,
    ...rest
}) => {

    const imageUrl = watch("imageUrl");

    const handleFileUpload = async (e) => {
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



            setValue("imagePublicId", res.data.publicId)
            setValue("imageUrl", res.data.url)



            // setValue("imagePublicId", res.data.publicId, {
            //     shouldValidate: true,
            //     shouldDirty: true
            // });


        } catch (err) {
            console.log(err);
            alert("Upload failed");
        } finally {
            setUploading(false);
        }

    };



    return (
        <>
            <CFormLabel>{label}</CFormLabel>

            <CFormInput
                type="file"
                invalid={errors.imageUrl}
                //invalid={!!errors[name]}
                // {...register(name)}
                onChange={handleFileUpload}
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

export default FormFileUpload;