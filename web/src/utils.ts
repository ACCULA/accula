import { History } from 'history'
import { IProject, IProjectConf, IUser } from 'types'

export const API_URL = process.env.REACT_APP_API_URL
export const DEBUG = false

export const drawerWidth = 260

export const isProjectAdmin = (
  user: IUser, //
  project: IProject,
  projectConf: IProjectConf
): boolean => {
  return (
    (user && project && project.creatorId === user.id) ||
    (user && projectConf && projectConf.admins.indexOf(user.id) !== -1)
  )
}

export const historyPush = (history: History, pathname: string) => {
  history.push(pathname, {
    from: history.location.pathname
  })
}
