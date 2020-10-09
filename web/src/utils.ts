import { History } from 'history'
import { IProject, IUser } from 'types'

export const API_URL = process.env.REACT_APP_API_URL
export const DEBUG = false
export const DRAWER_WIDTH = 260
export const DATE_TITLE_FORMAT = "d MMMM yyyy 'at' HH:mm"

export const isProjectAdmin = (
  user: IUser, //
  project: IProject
): boolean => {
  return (
    user && project && (project.creatorId === user.id || project.adminIds.indexOf(user.id) !== -1)
  )
}

export const historyPush = (history: History, pathname: string) => {
  history.push(pathname, {
    from: history.location.pathname
  })
}
