import { AppDispatch, AppStateSupplier } from 'store'
import { requireToken } from 'store/users/actions'
import { failed, fetched, fetching } from 'store/wrapper'
import { IProjectConf } from 'types'
import {
  SET_PROJECT,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  RESET_PROJECT_INFO,
  RESET_PROJECTS,
  SetProject,
  SetProjectConf,
  SetProjects,
  SetRepoAdmins,
  ResetProjectInfo,
  ResetProjects
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

export const resetProjectInfo = (): ResetProjectInfo => ({
  type: RESET_PROJECT_INFO
})

export const resetProjects = (): ResetProjects => ({
  type: RESET_PROJECTS
})

const setRepoAdmins = (payload): SetRepoAdmins => ({
  type: SET_REPO_ADMINS,
  payload
})

export const getProjectsAction = (handleError?: (message: string) => void) => async (
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
    if (handleError) {
      handleError(e.message)
    }
    dispatch(setProjects(failed(e)))
  }
}

export const getProjectAction = (id: number, handleError?: (msg: string) => void) => async (
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
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const getProjectConfAction = (id: number, handleError?: (msg: string) => void) => async (
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
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const updateProjectConfAction = (
  id: number,
  conf: IProjectConf,
  handleSuccess?: () => void,
  handleError?: (msg: string) => void
) => async (
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
    await putProjectConf(id, users.token, conf)
    if (handleSuccess) {
      handleSuccess()
    }
  } catch (e) {
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const getRepoAdminsAction = (id: number, handleError?: (msg: string) => void) => async (
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
    if (handleError) {
      handleError(e.message)
    }
  }
}

const messageFromError = (error: string): string => {
  switch (error) {
    case 'NO_PERMISSION':
      return 'Only the admin of the repository can create a project for it!'
    case 'WRONG_URL':
      return 'URL to the repository is wrong!'
    case 'ALREADY_EXISTS':
      return 'Project for this repository is already exists!'
    case 'INVALID_URL':
      return 'URL to the repository is invalid!'
    default:
      return 'Unknown error has occurred'
  }
}

export const createProjectAction = (
  url: string,
  handleSuccess?: () => void,
  handleError?: (msg: string) => void
) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, projects } = getState()
  if (users.token.accessToken) {
    const result = await postProject(url, users.token)
    if (typeof result === 'string') {
      if (handleError) {
        handleError(messageFromError(result))
      }
    } else {
      dispatch(setProjects(fetched([...projects.projects.value, result])))
      if (handleSuccess) {
        handleSuccess()
      }
    }
  }
}
