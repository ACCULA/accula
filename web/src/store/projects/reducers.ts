import { notFetching } from 'store/wrapper'
import {
  ProjectsActionTypes,
  ProjectsState,
  SET_PROJECT,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  UPDATE_PROJECT_CONF,
  RESET_PROJECT_INFO,
  RESET_PROJECTS
} from './types'

const initialState: ProjectsState = {
  projects: notFetching,
  project: notFetching,
  projectConf: notFetching,
  updateProjectConf: [false, null],
  repoAdmins: notFetching
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
    case SET_PROJECT_CONF: {
      return {
        ...state,
        projectConf: action.payload
      }
    }
    case UPDATE_PROJECT_CONF: {
      return {
        ...state,
        updateProjectConf: action.payload
      }
    }
    case SET_REPO_ADMINS: {
      return {
        ...state,
        repoAdmins: action.payload
      }
    }
    case RESET_PROJECT_INFO: {
      return {
        ...initialState,
        projects: state.projects
      }
    }
    case RESET_PROJECTS: {
      return initialState
    }
    default:
      return state
  }
}
