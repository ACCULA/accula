import UserProfile from 'views/UserProfile'
import PullRequestList from 'views/PullRequestList'
import { RouteInfo } from 'types'

const routes: RouteInfo[] = [
  {
    path: '/',
    exact: true,
    name: 'Pull Requests',
    icon: 'pe-7s-note2',
    component: PullRequestList
  },
  {
    path: '/profile',
    name: 'Profile',
    icon: 'pe-7s-user',
    component: UserProfile
  }
]

export default routes
