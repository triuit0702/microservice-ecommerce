import React from 'react'

import Page403 from './views/errors/Page403'
import ProductForm from './views/products/ProductForm'
import CategoryManager from './views/category/CategoryList'
import CategoryForm from './views/category/CategoryForm'

const UserManagement = React.lazy(() => import('./views/users/UserManagement'))
const ProductList = React.lazy(() => import('./views/products/ProductList'))
const AuditLogList = React.lazy(() => import('./views/audit/AuditLogList'))

const routes = [
  { path: '/', exact: true, name: 'Home' },

  { path: '/users', name: 'Users', element: UserManagement },

  { path: '/products', name: 'Products', element: ProductList },
  { path: '/products/add', name: 'Add Product', element: ProductForm },
  { path: '/products/edit/:id', name: 'Edit Product', element: ProductForm },

  { path: '/categories', name: 'Categories', element: CategoryManager },
  { path: '/categories/add', name: 'Add Category', element: CategoryForm },
  { path: '/categories/edit/:id', name: 'Edit Category', element: CategoryForm },

  { path: '/audit-logs', name: 'Audit Log', element: AuditLogList },

  { path: '/403', name: '403 Forbidden', element: Page403 },
]

export default routes
