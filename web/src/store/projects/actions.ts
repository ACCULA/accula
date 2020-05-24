import { AppDispatch, AppStateSupplier } from 'store'
import { IProject } from 'types'
import {
  FETCHING_PROJECTS,
  FetchingProjects,
  SET_PROJECT,
  SET_PROJECTS,
  SetProject,
  SetProjects
} from './types'
import { createProject, getProject, getProjects } from './services'

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
  const { users, projects } = getState()
  if (users.token) {
    const project = await createProject(url, users.token)
    if (project) {
      dispatch(setProjects([...projects.projects, project]))
    }
  }
}
