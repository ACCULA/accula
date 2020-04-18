import UserProfile from 'views/UserProfile'
import PullRequestList from 'views/PullRequestList'
import { RouteInfo } from 'types'
import Login from 'components/Login/Login'

const routes: RouteInfo[] = [
  {
    path: '/',
    exact: true,
    name: 'Pull Requests',
    icon: 'list-ul',
    component: PullRequestList
  },
  {
    path: '/profile',
    name: 'Profile',
    icon: 'user',
    component: UserProfile
  },
  {
    path: '/login',
    name: 'Login',
    icon: 'sign-in-alt',
    component: Login,
    hidden: true
  }
]

export default routes
