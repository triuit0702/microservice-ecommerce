import React, { useEffect, useState } from "react";
import {
    CCard,
    CCardBody,
    CCardHeader,
    CTable,
    CTableHead,
    CTableRow,
    CTableHeaderCell,
    CTableBody,
    CTableDataCell,
    CButton,
    CImage,
} from "@coreui/react";

import { useNavigate } from 'react-router-dom';
import { serviceGetAllCategories } from "../../services/CategoryService";

const CategoryList = () => {
    const navigate = useNavigate();
    const [idCategory, setIdCategory] = useState('');


    const [categories, setCategories] = useState([
        // { id: "1", name: "Electronics", parentId: null, imageUrl: "" },
        // { id: "2", name: "Laptop", parentId: "1", imageUrl: "" },
        // { id: "3", name: "Phone", parentId: "1", imageUrl: "" },
        // { id: "4", name: "Apple", parentId: "3", imageUrl: "" },
    ]);

    // load categories from the server
    useEffect(() => {
        // TODO: fetch from the backend API
        // setCategories([
        //     { id: "1", name: "Electronics", parentId: null },
        //     { id: "2", name: "Laptop", parentId: "1" },
        //     { id: "3", name: "Phone", parentId: "1" },
        // ]);

        serviceGetAllCategories().then((res) => {
            let data = res.data.data;
            setCategories(data);
        })
    }, []);

    // parent name
    const getParentName = (parentId) => {
        const parent = categories.find((c) => c.id === parentId);
        return parent ? parent.name : "—";
    };

    // render as a tree inside the table
    const renderRows = (parentId = null, level = 0) => {
        return categories
            .filter((c) => c.parentId === parentId)
            .map((c) => (
                <React.Fragment key={c.id}>
                    <CTableRow>
                        <CTableDataCell>
                            {c.imageUrl ? (
                                <CImage src={c.imageUrl} width={32} height={32} />
                            ) : (
                                "📂"
                            )}
                        </CTableDataCell>

                        <CTableDataCell>
                            <span style={{ marginLeft: level * 20 }}>
                                {level > 0 && "└─ "}
                                {c.name}
                            </span>
                        </CTableDataCell>

                        <CTableDataCell>{getParentName(c.parentId)}</CTableDataCell>

                        <CTableDataCell>
                            <CButton size="sm"
                                onClick={() => navigate(`/categories/edit/${c.id}`)}
                                color="warning" className="me-2">
                                Edit
                            </CButton>
                            <CButton size="sm" color="danger">
                                Delete
                            </CButton>
                        </CTableDataCell>
                    </CTableRow>

                    {renderRows(c.id, level + 1)}
                </React.Fragment>
            ));
    };

    return (
        <CCard>



            <CCardHeader className="d-flex justify-content-between align-items-center">
                <strong>Category List</strong>

                <CButton color="primary" onClick={() => navigate('/categories/add')}>Add Category</CButton>
            </CCardHeader>

            <CCardBody>
                <CTable hover responsive align="middle">
                    <CTableHead color="light">
                        <CTableRow>
                            <CTableHeaderCell style={{ width: "80px" }}>
                                Image
                            </CTableHeaderCell>
                            <CTableHeaderCell>Name</CTableHeaderCell>
                            <CTableHeaderCell style={{ width: "200px" }}>
                                Parent
                            </CTableHeaderCell>
                            <CTableHeaderCell style={{ width: "180px" }}>
                                Actions
                            </CTableHeaderCell>
                        </CTableRow>
                    </CTableHead>

                    <CTableBody>{renderRows()}</CTableBody>
                </CTable>
            </CCardBody>
        </CCard>
    );
};

export default CategoryList;
