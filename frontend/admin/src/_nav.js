import React from 'react'
import CIcon from '@coreui/icons-react'
import { cilCalculator, cilFolder, cilHistory, cilUser } from '@coreui/icons'
import { CNavItem } from '@coreui/react'
import { USER_READ } from './constants/permissions'

const _nav = [
  {
    component: CNavItem,
    name: 'Products',
    to: '/products',
    icon: <CIcon icon={cilCalculator} customClassName="nav-icon" />,
  },
  {
    component: CNavItem,
    name: 'Categories',
    to: '/categories',
    icon: <CIcon icon={cilFolder} customClassName="nav-icon" />,
  },
  {
    component: CNavItem,
    name: 'Users',
    to: '/users',
    icon: <CIcon icon={cilUser} customClassName="nav-icon" />,
    // shown only when the user has this permission (filtered in AppSidebar)
    permission: USER_READ,
  },
  {
    component: CNavItem,
    name: 'Audit Log',
    to: '/audit-logs',
    icon: <CIcon icon={cilHistory} customClassName="nav-icon" />,
    // audit log only covers user management, so it uses the same permission
    permission: USER_READ,
  },
]

export default _nav
