// Permission codes, same values as permissions.name in backend/env/mysql/init/init.sql.
// UI only: the backend always checks permissions again.
export const USER_READ = 'user:read'
export const USER_CREATE = 'user:create'
export const USER_UPDATE = 'user:update'
export const USER_DELETE = 'user:delete'

export const PRODUCT_READ = 'product:read'
export const PRODUCT_CREATE = 'product:create'
export const PRODUCT_UPDATE = 'product:update'
export const PRODUCT_DELETE = 'product:delete'

// Roles allowed to use the admin app
export const STAFF_ROLES = ['ADMINISTRATOR', 'EMPLOYEE']

export const hasPermission = (user, permission) => !!user?.permissions?.includes(permission)

export const isStaff = (user) => !!user?.roles?.some((role) => STAFF_ROLES.includes(role))
