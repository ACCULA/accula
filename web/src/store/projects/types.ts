import { IProject, IProjectConf, IProjectRef, IUser } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const SET_PROJECT_CONF = 'SET_PROJECT_CONF'
export const UPDATE_PROJECT_CONF = 'UPDATE_PROJECT_CONF'
export const SET_REPO_ADMINS = 'SET_REPO_ADMINS'
export const RESET_PROJECT_INFO = 'RESET_PROJECT_INFO'
export const RESET_PROJECTS = 'RESET_PROJECTS'

export interface ProjectsState {
  projects: Wrapper<IProject[]>
  project: Wrapper<IProject>
  projectConf: Wrapper<IProjectConf> & IProjectRef
  updateProjectConf: [boolean, string]
  repoAdmins: Wrapper<IUser[]> & IProjectRef
}

export interface SetProjects {
  type: typeof SET_PROJECTS
  payload: Wrapper<IProject[]>
}

export interface SetProject {
  type: typeof SET_PROJECT
  payload: Wrapper<IProject>
}

export interface SetProjectConf {
  type: typeof SET_PROJECT_CONF
  payload: Wrapper<IProjectConf> & IProjectRef
}

export interface UpdateProjectConf {
  type: typeof UPDATE_PROJECT_CONF
  payload: [boolean, string]
}

export interface SetRepoAdmins {
  type: typeof SET_REPO_ADMINS
  payload: Wrapper<IUser[]> & IProjectRef
}
export interface ResetProjectInfo {
  type: typeof RESET_PROJECT_INFO
}

export interface ResetProjects {
  type: typeof RESET_PROJECTS
}

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | SetProjectConf
  | UpdateProjectConf
  | SetRepoAdmins
  | ResetProjectInfo
  | ResetProjects
