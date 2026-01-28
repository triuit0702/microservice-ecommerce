import React from 'react'

const Dashboard = React.lazy(() => import('./views/dashboard/Dashboard'))

// user
const UserManagement = React.lazy(() => import('./views/users/UserManagement'))

const Colors = React.lazy(() => import('./views/theme/colors/Colors'))

const ProductList = React.lazy(() => import('./views/products/ProductList'))

import Page403 from "./views/errors/Page403";
import ProductForm from './views/products/ProductForm'
import CategoryManager from './views/category/CategoryList'
import CategoryForm from './views/category/CategoryForm'








const routes = [
  {
    path: '/users',
    name: 'Users',
    element: UserManagement,
  },

  { path: '/', exact: true, name: 'Home' },
  { path: '/dashboard', name: 'Dashboard', element: Dashboard },
  { path: '/theme', name: 'Theme', element: Colors, exact: true },
  { path: '/theme/colors', name: 'Colors', element: Colors },

  { path: '/products', name: 'Colors', element: ProductList },
  { path: '/products/add', name: 'Add Product', element: ProductForm },
  { path: '/products/edit/:id', name: 'Edit Product', element: ProductForm },

  { path: '/categories', name: 'Colors', element: CategoryManager },
  { path: '/categories/add', name: 'Colors', element: CategoryForm },

  { path: '/categories/edit/:id', name: 'Colors', element: CategoryForm },

  { path: "/403", name: "403 Forbidden", element: Page403 },

]

export default routes
