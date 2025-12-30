export const getErrorMessage = (error) => {
    if (!error?.response) {
        return 'Không thể kết nối server'
    }

    const { status, data } = error.response

    // Ưu tiên message từ backend
    if (data?.message) {
        return data.message
    }

    switch (status) {
        case 400:
            return 'Dữ liệu không hợp lệ'
        case 401:
            return 'Bạn chưa đăng nhập'
        case 403:
            return 'Bạn không có quyền'
        case 404:
            return 'Không tìm thấy dữ liệu'
        case 500:
            return 'Lỗi hệ thống'
        default:
            return 'Có lỗi xảy ra'
    }
}
