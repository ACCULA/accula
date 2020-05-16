import { RouteInfo } from 'types'

import UserProfile from 'views/UserProfile'
import Projects from 'views/Projects'
import Settings from 'views/Settings'
import OAuth2RedirectHandler from 'views/OAuth2RedirectHandler'

export const routes: RouteInfo[] = [
  {
    path: '/projects',
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
    icon: 'cog',
    component: Settings
  },
  {
    path: '/oauth2/redirect',
    name: 'Settings',
    component: OAuth2RedirectHandler,
    hidden: true,
    exact: true
  }
]
