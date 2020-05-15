import { AppDispatch, AppStateSupplier } from 'store'
import { Project } from 'types'
import {
  FETCHING_PROJECTS,
  FetchingProjects,
  SET_PROJECT,
  SET_PROJECTS,
  SetProject,
  SetProjects
} from 'store/projects/types'
import { createProject, getProjects } from 'services/projectsService'

const setProjects = (projects: Project[]): SetProjects => ({
  type: SET_PROJECTS,
  projects
})

const setProject = (project: Project): SetProject => ({
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
  if (!projects.projects) {
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
}

export const getProjectAction = (id: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.projects) {
    const project = projects.projects.find(p => p.id === id)
    if (project) {
      dispatch(setProject(project))
    }
  }
}

export const createProjectAction = (url: string) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { users, projects } = getState()
  if (users.token) {
    const project = await createProject(url, users.token.accessToken)
    if (project) {
      dispatch(setProjects([...projects.projects, project]))
    }
  }
}
