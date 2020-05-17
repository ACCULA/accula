import { IProject } from 'types'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const FETCHING_PROJECTS = 'FETCHING_PROJECTS'

export interface ProjectsState {
  projects?: IProject[]
  project?: IProject
  isFetching: boolean
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

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | FetchingProjects