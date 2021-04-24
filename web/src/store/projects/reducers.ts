import { notFetching } from 'store/wrapper'
import {
  ProjectsActionTypes,
  ProjectsState,
  SET_BASE_FILES,
  SET_PROJECT,
  SET_TOP_PLAGIARISTS,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  RESET_PROJECT_INFO,
  RESET_PROJECTS
} from './types'

const initialState: ProjectsState = {
  projects: notFetching,
  project: notFetching,
  topPlagiarists: notFetching,
  projectConf: notFetching,
  repoAdmins: notFetching,
  baseFiles: notFetching
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
    case SET_TOP_PLAGIARISTS: {
      return {
        ...state,
        topPlagiarists: action.payload
      }
    }
    case SET_PROJECT_CONF: {
      return {
        ...state,
        projectConf: action.payload
      }
    }
    case SET_REPO_ADMINS: {
      return {
        ...state,
        repoAdmins: action.payload
      }
    }
    case SET_BASE_FILES: {
      return {
        ...state,
        baseFiles: action.payload
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
