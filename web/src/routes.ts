import { IRouteInfo } from 'types'

import UserProfile from 'views/UserProfile'
import Settings from 'views/Settings'
import OAuth2RedirectHandler from 'views/OAuth2RedirectHandler'
import { ProjectsRoutes } from 'views/Projects/ProjectsRoutes'

export const routes: IRouteInfo[] = [
  {
    path: '/projects',
    name: 'Projects',
    icon: 'list-ul',
    component: ProjectsRoutes
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
