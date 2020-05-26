import { IProject } from 'types'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const FETCHING_PROJECTS = 'FETCHING_PROJECTS'
export const SET_CREATION_STATE = 'SET_CREATION_STATE'

export interface ProjectsState {
  projects?: IProject[]
  project?: IProject
  isFetching: boolean
  creationState: [boolean, string]
}

export interface SetProjects {
  type: typeof SET_PROJECTS
  projects: IProject[]
}

export interface SetProject {
  type: typeof SET_PROJECT
  project: IProject
}

export interface FetchingProjects {
  type: typeof FETCHING_PROJECTS
  isFetching: boolean
}

export interface SetCreationState {
  type: typeof SET_CREATION_STATE
  creationState: [boolean, string]
}

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | FetchingProjects
  | SetCreationState
