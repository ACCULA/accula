import { IProject } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PROJECTS = 'SET_PROJECTS'
export const SET_PROJECT = 'SET_PROJECT'
export const SET_CREATION_STATE = 'SET_CREATION_STATE'

export interface ProjectsState {
  projects: Wrapper<IProject[]>
  project: Wrapper<IProject>
  creationState: [boolean, string]
}

export interface SetProjects {
  type: typeof SET_PROJECTS
  payload: Wrapper<IProject[]>
}

export interface SetProject {
  type: typeof SET_PROJECT
  payload: Wrapper<IProject>
}

export interface SetCreationState {
  type: typeof SET_CREATION_STATE
  creationState: [boolean, string]
}

export type ProjectsActionTypes =
  | SetProjects //
  | SetProject
  | SetCreationState
