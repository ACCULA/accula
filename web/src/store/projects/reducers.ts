import { notFetching } from 'store/wrapper'
import {
  CREATE_PROJECT,
  ProjectsActionTypes,
  ProjectsState,
  SET_PROJECT,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  UPDATE_PROJECT_CONF
} from './types'

const initialState: ProjectsState = {
  projects: notFetching,
  project: notFetching,
  projectConf: notFetching,
  updateProjectConf: [false, null],
  repoAdmins: notFetching,
  createProject: [false, null]
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
    case CREATE_PROJECT: {
      return {
        ...state,
        createProject: action.payload
      }
    }
    default:
      return state
  }
}
