import { notFetching } from 'store/wrapper'
import {
  ProjectsActionTypes,
  ProjectsState,
  SET_PROJECT,
  SET_TOP_PLAGIARISTS,
  SET_TOP_CLONE_SOURCES,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  RESET_PROJECT_INFO,
  RESET_PROJECTS
} from './types'

const initialState: ProjectsState = {
  projects: notFetching,
  project: notFetching,
  topPlagiarists: notFetching,
  topCloneSources: notFetching,
  projectConf: notFetching
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
    case SET_TOP_CLONE_SOURCES: {
      return {
        ...state,
        topCloneSources: action.payload
      }
    }
    case SET_PROJECT_CONF: {
      return {
        ...state,
        projectConf: action.payload
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
