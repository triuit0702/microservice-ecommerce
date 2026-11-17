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
                <CModalTitle>Confirm delete</CModalTitle>
            </CModalHeader>

            <CModalBody>
                {message}
            </CModalBody>

            <CModalFooter>
                <CButton
                    color="secondary"
                    onClick={() => hideModalConfirm()}
                >
                    Cancel
                </CButton>

                <CButton
                    color="danger"
                    onClick={handleConfirmDelete}
                >
                    Delete
                </CButton>
            </CModalFooter>
        </CModal>

    );
};

export default ModalConfirm;
