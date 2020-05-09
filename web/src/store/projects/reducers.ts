import { ProjectsActionTypes, ProjectsState, SET_PROJECT, SET_PROJECTS } from 'store/projects/types'

const initialState: ProjectsState = {
  projects: null,
  project: null,
  isFetching: false
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
    default:
      return state
  }
}
