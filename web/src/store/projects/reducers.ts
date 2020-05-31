import { notFetching } from 'store/wrapper'
import {
  ProjectsActionTypes,
  ProjectsState,
  SET_CREATION_STATE,
  SET_PROJECT,
  SET_PROJECTS
} from './types'

const initialState: ProjectsState = {
  projects: notFetching,
  project: notFetching,
  creationState: [false, null]
}

export function projectsReducer(
  state: ProjectsState = initialState, //
  action: ProjectsActionTypes
): ProjectsState {
  switch (action.type) {
    case SET_PROJECTS: {
      return {
        ...state,
        projects: action.payload
      }
    }
    case SET_PROJECT: {
      return {
        ...state,
        project: action.payload
      }
    }
    case SET_CREATION_STATE: {
      return {
        ...state,
        creationState: action.creationState
      }
    }
    default:
      return state
  }
}
