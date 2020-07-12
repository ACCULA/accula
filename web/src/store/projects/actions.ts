import { AppDispatch, AppStateSupplier } from 'store'
import { requireToken } from 'store/users/actions'
import { failed, fetched, fetching } from 'store/wrapper'
import {
  SET_CREATION_STATE,
  SET_PROJECT,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  SetCreationState,
  SetProject,
  SetProjects,
  SetRepoAdmins
} from './types'
import { createProject, getProject, getProjects, getRepoAdmins } from './services'

const setProjects = (payload): SetProjects => ({
  type: SET_PROJECTS,
  payload
})

const setProject = (payload): SetProject => ({
  type: SET_PROJECT,
  payload
})

const setRepoAdmins = (payload): SetRepoAdmins => ({
  type: SET_REPO_ADMINS,
  payload
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
  if (projects.projects.value) {
    return
  }
  try {
    dispatch(setProjects(fetching))
    const result = await getProjects()
    dispatch(setProjects(fetched(result)))
  } catch (e) {
    dispatch(setProjects(failed(e)))
  }
}

export const getProjectAction = (id: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.project.value && projects.project.value.id === id) {
    return
  }
  if (projects.projects.value) {
    const project = projects.projects.value.find(p => p.id === id)
    if (project) {
      dispatch(setProject(fetched(project)))
      return
    }
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    dispatch(setProject(fetching))
    const project = await getProject(id, users.token)
    dispatch(setProject(fetched(project)))
  } catch (e) {
    dispatch(setProjects(failed(e)))
  }
}

export const getRepoAdminsAction = (id: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.repoAdmins.value && projects.repoAdmins.projectId === id) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    dispatch(setRepoAdmins(fetching))
    const admins = await getRepoAdmins(id, users.token)
    dispatch(setRepoAdmins(fetched(admins)))
  } catch (e) {
    dispatch(setRepoAdmins(failed(e)))
  }
}

export const createProjectAction = (url: string) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, projects } = getState()
  if (users.token.accessToken) {
    const result = await createProject(url, users.token)
    if (typeof result === 'string') {
      dispatch(setCreationState(false, result))
    } else {
      dispatch(setProjects(fetched([...projects.projects.value, result])))
      dispatch(setCreationState(false, null))
    }
  }
}
