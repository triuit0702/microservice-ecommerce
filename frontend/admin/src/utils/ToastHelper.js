export const getErrorMessage = (error) => {
    if (!error?.response) {
        return 'Cannot connect to the server'
    }

    const { status, data } = error.response

    // prefer the backend message
    if (data?.message) {
        return data.message
    }

    if (data?.error?.message) {
        return data.error.message;
    }

    switch (status) {
        case 400:
            return 'Invalid data'
        case 401:
            return 'You are not logged in'
        case 403:
            return 'You do not have permission'
        case 404:
            return 'Data not found'
        case 500:
            return 'System error'
        default:
            return 'Something went wrong'
    }
}
