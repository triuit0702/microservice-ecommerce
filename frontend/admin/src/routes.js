import React from 'react'

const Dashboard = React.lazy(() => import('./views/dashboard/Dashboard'))

// user
const UserManagement = React.lazy(() => import('./views/users/UserManagement'))

const Colors = React.lazy(() => import('./views/theme/colors/Colors'))

const ProductList = React.lazy(() => import('./views/products/ProductList'))

import Page403 from "./views/errors/Page403";








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

  { path: "/403", name: "403 Forbidden", element: Page403 },

]

export default routes
