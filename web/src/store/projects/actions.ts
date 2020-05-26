import { AppDispatch, AppStateSupplier } from 'store'
import { IProject } from 'types'
import {
  FETCHING_PROJECTS,
  FetchingProjects,
  SET_CREATION_STATE,
  SET_PROJECT,
  SET_PROJECTS,
  SetCreationState,
  SetProject,
  SetProjects
} from './types'
import { createProject, getProject, getProjects } from './services'
import { requireToken } from 'store/users/actions'

const setProjects = (projects: IProject[]): SetProjects => ({
  type: SET_PROJECTS,
  projects
})

const setProject = (project: IProject): SetProject => ({
  type: SET_PROJECT,
  project
})

const fetchingProjects = (isFetching: boolean): FetchingProjects => ({
  type: FETCHING_PROJECTS,
  isFetching
})

const setCreationState = (isCreating: boolean, error: string): SetCreationState => ({
  type: SET_CREATION_STATE,
  creationState: [isCreating, error]
})

export const resetCreationStateAction = (): SetCreationState => setCreationState(false, '')

export const getProjectsAction = () => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.projects) {
    return
  }
  try {
    dispatch(fetchingProjects(true))
    const projs = await getProjects()
    dispatch(setProjects(projs))
  } catch (e) {
    console.log(e)
    dispatch(setProjects([]))
  } finally {
    dispatch(fetchingProjects(false))
  }
}

export const getProjectAction = (id: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.project && projects.project.id === id) {
    return
  }
  if (projects.projects) {
    const project = projects.projects.find(p => p.id === id)
    if (project) {
      dispatch(setProject(project))
      return
    }
  }
  try {
    dispatch(fetchingProjects(true))
    const project = await getProject(id)
    dispatch(setProject(project))
  } catch (e) {
    console.log(e)
    dispatch(setProjects([]))
  } finally {
    dispatch(fetchingProjects(false))
  }
}

export const createProjectAction = (url: string) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, projects } = getState()
  if (!users.token) {
    return
  }
  if (users.token) {
    const result = await createProject(url, users.token)
    if (typeof result === 'string') {
      dispatch(setCreationState(false, result))
    } else {
      dispatch(setProjects([...projects.projects, result]))
      dispatch(setCreationState(false, null))
    }
  } else {
    console.log('else')
  }
}
