import { IProject, IProjectConf, IProjectRef, ICloneStatistics } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const SET_TOP_PLAGIARISTS = 'SET_TOP_PLAGIARISTS'
export const SET_TOP_CLONE_SOURCES = 'SET_TOP_CLONE_SOURCES'
export const SET_PROJECT_CONF = 'SET_PROJECT_CONF'
export const RESET_PROJECT_INFO = 'RESET_PROJECT_INFO'
export const RESET_PROJECTS = 'RESET_PROJECTS'
export const CREATE_PROJECT = 'CREATE_PROJECT'

export interface ProjectsState {
  projects: Wrapper<IProject[]>
  project: Wrapper<IProject>
  topPlagiarists: Wrapper<ICloneStatistics[]> & IProjectRef
  topCloneSources: Wrapper<ICloneStatistics[]> & IProjectRef
  projectConf: Wrapper<IProjectConf> & IProjectRef
}

export interface SetProjects {
  type: typeof SET_PROJECTS
  payload: Wrapper<IProject[]>
}

export interface SetProject {
  type: typeof SET_PROJECT
  payload: Wrapper<IProject>
}

export interface SetTopPlagiarists {
  type: typeof SET_TOP_PLAGIARISTS
  payload: Wrapper<ICloneStatistics[]>
}

export interface SetTopCloneSources {
  type: typeof SET_TOP_CLONE_SOURCES
  payload: Wrapper<ICloneStatistics[]>
}

export interface SetProjectConf {
  type: typeof SET_PROJECT_CONF
  payload: Wrapper<IProjectConf> & IProjectRef
}

export interface ResetProjectInfo {
  type: typeof RESET_PROJECT_INFO
}

export interface ResetProjects {
  type: typeof RESET_PROJECTS
}

export interface CreateProject {
  type: typeof CREATE_PROJECT
  payload: [boolean, string]
}

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | SetTopPlagiarists
  | SetTopCloneSources
  | SetProjectConf
  | ResetProjectInfo
  | ResetProjects
  | CreateProject
