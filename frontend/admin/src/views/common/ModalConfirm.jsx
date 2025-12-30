import {
    CButton,
    CModal,
    CModalBody,
    CModalFooter,
    CModalHeader,
    CModalTitle,
} from "@coreui/react";

const ModalConfirm = ({ visibleDelete, message, hideModalConfirm, deleteUser }) => {

    const handleConfirmDelete = async () => {
        deleteUser();
    }
    return (
        <CModal
            visible={visibleDelete}
            onClose={() => hideModalConfirm()}
            alignment="center"
        >
            <CModalHeader>
                <CModalTitle>Xác nhận xóa</CModalTitle>
            </CModalHeader>

            <CModalBody>
                {message}
            </CModalBody>

            <CModalFooter>
                <CButton
                    color="secondary"
                    onClick={() => setVisibleDelete(false)}
                >
                    Hủy
                </CButton>

                <CButton
                    color="danger"
                    onClick={handleConfirmDelete}
                >
                    Xóa
                </CButton>
            </CModalFooter>
        </CModal>

    );
};

export default ModalConfirm;
