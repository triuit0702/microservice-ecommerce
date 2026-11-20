// The client app is for CUSTOMER accounts only; staff (ADMINISTRATOR, EMPLOYEE) use the admin app.
// UI only: the gateway also blocks staff from cart and order APIs.
export const STAFF_ACCOUNT_MESSAGE = "This is a staff account. Please use the admin site."

export const isCustomer = (user) => !!user?.roles?.includes("CUSTOMER")
