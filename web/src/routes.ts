import { IRouteInfo } from 'types'

import UserProfile from 'views/UserProfile'
import Settings from 'views/Settings'
import OAuth2RedirectHandler from 'views/OAuth2RedirectHandler'
import { ProjectsRoutes } from 'views/Projects/ProjectsRoutes'

export const routes: IRouteInfo[] = [
  {
    path: '/projects',
    name: 'Projects',
    component: ProjectsRoutes,
    icon: 'list-ul'
  },
  {
    path: '/profile',
    name: 'Profile',
    component: UserProfile,
    icon: 'user',
    authRequired: true
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    icon: 'cog',
    authRequired: true
  },
  {
    path: '/oauth2/redirect',
    name: 'Settings',
    component: OAuth2RedirectHandler,
    hidden: true,
    exact: true
  }
]
