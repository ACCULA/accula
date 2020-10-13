import { IProject, IProjectConf, IProjectRef, IUser } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const SET_PROJECT_CONF = 'SET_PROJECT_CONF'
export const SET_REPO_ADMINS = 'SET_REPO_ADMINS'
export const RESET_PROJECT_INFO = 'RESET_PROJECT_INFO'
export const RESET_PROJECTS = 'RESET_PROJECTS'
export const SET_BASE_FILES = 'SET_BASE_FILES'
export const CREATE_PROJECT = 'CREATE_PROJECT'

export interface ProjectsState {
  projects: Wrapper<IProject[]>
  project: Wrapper<IProject>
  projectConf: Wrapper<IProjectConf> & IProjectRef
  repoAdmins: Wrapper<IUser[]> & IProjectRef
  baseFiles: Wrapper<string[]> & IProjectRef
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
export interface SetBaseFiles {
  type: typeof SET_BASE_FILES
  payload: Wrapper<string[]> & IProjectRef
}

export interface CreateProject {
  type: typeof CREATE_PROJECT
  payload: [boolean, string]
}

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | SetProjectConf
  | SetRepoAdmins
  | ResetProjectInfo
  | ResetProjects
  | SetBaseFiles
  | CreateProject
