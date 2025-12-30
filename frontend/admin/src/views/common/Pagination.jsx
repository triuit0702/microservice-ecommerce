import {
    CPagination,
    CPaginationItem
} from "@coreui/react";

const Pagination = ({ page, totalPages, onPageChange }) => {
    if (totalPages <= 1) return null;

    return (
        <CPagination align="center" className="mt-3">
            {/* Prev */}
            <CPaginationItem
                disabled={page === 0}
                onClick={() => onPageChange(page - 1)}
            >
                Prev
            </CPaginationItem>

            {[...Array(totalPages)].map((_, index) => (
                <CPaginationItem
                    key={index}
                    active={index === page}
                    onClick={() => onPageChange(index)}
                >
                    {index + 1}
                </CPaginationItem>
            ))}

            {/* Next */}
            <CPaginationItem
                disabled={page === totalPages - 1}
                onClick={() => onPageChange(page + 1)}
            >
                Next
            </CPaginationItem>
        </CPagination>
    );
};

export default Pagination;
