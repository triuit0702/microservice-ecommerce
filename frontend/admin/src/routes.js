import React from 'react'

const Dashboard = React.lazy(() => import('./views/dashboard/Dashboard'))

// user
const UserManagement = React.lazy(() => import('./views/users/UserManagement'))

const Colors = React.lazy(() => import('./views/theme/colors/Colors'))










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

]

export default routes
