import { Project } from 'types'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const FETCHING_PROJECTS = 'FETCHING_PROJECTS'

export interface ProjectsState {
  projects?: Project[]
  project?: Project
  isFetching: boolean
}

export interface SetProjects {
  type: typeof SET_PROJECTS
  projects: Project[]
}

export interface SetProject {
  type: typeof SET_PROJECT
  project: Project
}

export interface FetchingProjects {
  type: typeof FETCHING_PROJECTS
  isFetching: boolean
}

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | FetchingProjects
