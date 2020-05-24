import {
  FETCHING_PROJECTS,
  ProjectsActionTypes,
  ProjectsState,
  SET_CREATION_STATE,
  SET_PROJECT,
  SET_PROJECTS
} from 'store/projects/types'

const initialState: ProjectsState = {
  projects: null,
  project: null,
  isFetching: false,
  creationState: [false, null]
}

export function projectsReducer(
  state = initialState, //
  action: ProjectsActionTypes
): ProjectsState {
  switch (action.type) {
    case SET_PROJECTS: {
      return {
        ...state,
        projects: action.projects
      }
    }
    case SET_PROJECT: {
      return {
        ...state,
        project: action.project
      }
    }
    case FETCHING_PROJECTS: {
      return {
        ...state,
        isFetching: action.isFetching
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
