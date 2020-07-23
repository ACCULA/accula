import { AppDispatch, AppStateSupplier } from 'store'
import { requireToken } from 'store/users/actions'
import { failed, fetched, fetching } from 'store/wrapper'
import { IProjectConf } from 'types'
import {
  CREATE_PROJECT,
  SET_PROJECT,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  UPDATE_PROJECT_CONF,
  CreateProject,
  SetProject,
  SetProjectConf,
  SetProjects,
  SetRepoAdmins,
  UpdateProjectConf
} from './types'
import {
  postProject,
  getProject,
  getProjectConf,
  getProjects,
  getRepoAdmins,
  putProjectConf
} from './services'

const setProjects = (payload): SetProjects => ({
  type: SET_PROJECTS,
  payload
})

const setProject = (payload): SetProject => ({
  type: SET_PROJECT,
  payload
})

const setProjectConf = (payload): SetProjectConf => ({
  type: SET_PROJECT_CONF,
  payload
})

const updateProjectConf = (isCreating: boolean, error: string): UpdateProjectConf => ({
  type: UPDATE_PROJECT_CONF,
  payload: [isCreating, error]
})

export const resetUpdateProjectConf = (): UpdateProjectConf => updateProjectConf(false, '')

const setRepoAdmins = (payload): SetRepoAdmins => ({
  type: SET_REPO_ADMINS,
  payload
})

const createProject = (isCreating: boolean, error: string): CreateProject => ({
  type: CREATE_PROJECT,
  payload: [isCreating, error]
})

export const resetCreateProject = (): CreateProject => createProject(false, '')

export const getProjectsAction = () => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.projects.isFetching || projects.projects.value) {
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
  if (projects.project.isFetching || (projects.project.value && projects.project.value.id === id)) {
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

export const getProjectConfAction = (id: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (
    projects.projectConf.isFetching ||
    (projects.projectConf.value && projects.projectConf.projectId === id)
  ) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    dispatch(setProjectConf(fetching))
    const conf = await getProjectConf(id, users.token)
    dispatch(setProjectConf(fetched(conf)))
  } catch (e) {
    dispatch(setProjectConf(failed(e)))
  }
}

export const updateProjectConfAction = (id: number, conf: IProjectConf) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.projectConf.value && projects.projectConf.projectId === id) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    dispatch(updateProjectConf(true, null))
    await putProjectConf(id, users.token, conf)
    dispatch(updateProjectConf(false, null))
  } catch (e) {
    dispatch(updateProjectConf(false, e.message))
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
    const result = await postProject(url, users.token)
    if (typeof result === 'string') {
      dispatch(createProject(false, result))
    } else {
      dispatch(setProjects(fetched([...projects.projects.value, result])))
      dispatch(createProject(false, null))
    }
  }
}
