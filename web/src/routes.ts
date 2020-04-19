import UserProfile from 'views/UserProfile'
import Projects from 'views/Projects'
import { RouteInfo } from 'types'
import Settings from 'views/Settings'

const routes: RouteInfo[] = [
  {
    path: '/',
    exact: true,
    name: 'Projects',
    icon: 'list-ul',
    component: Projects
  },
  {
    path: '/profile',
    name: 'Profile',
    icon: 'user',
    component: UserProfile
  },
  {
    path: '/settings',
    name: 'Settings',
    icon: 'cogs',
    component: Settings
  }
]

export default routes
