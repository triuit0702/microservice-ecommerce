import React from "react";
import { TextField } from "@mui/material";
import { Controller } from "react-hook-form";

const CommonTextField = ({
    name, control, label, rules
}) => {
    return (
        <Controller
            name={name}
            control={control}
            rules={rules}
            render={({ field, fieldState: { error } }) => (
                <TextField
                    {...field}
                    label={label}
                    fullWidth
                    margin="normal"
                    error={!!error}
                    helperText={error ? error.message : ""}
                />
            )}
        />

    )
}

export default CommonTextField