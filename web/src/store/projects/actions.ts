import { AppDispatch, AppStateSupplier } from 'store'
import { requireToken } from 'store/users/actions'
import { failed, fetched, fetching } from 'store/wrapper'
import { IProjectConf } from 'types'
import {
  SET_BASE_FILES,
  SET_PROJECT,
  SET_TOP_PLAGIARISTS,
  SET_TOP_CLONE_SOURCES,
  SET_PROJECT_CONF,
  SET_PROJECTS,
  SET_REPO_ADMINS,
  RESET_PROJECT_INFO,
  RESET_PROJECTS,
  SetBaseFiles,
  SetProject,
  SetTopPlagiarists,
  SetTopCloneSources,
  SetProjectConf,
  SetProjects,
  SetRepoAdmins,
  ResetProjectInfo,
  ResetProjects,
} from './types'
import {
  getBaseFiles,
  getProject,
  getTopPlagiarists,
  getProjectConf,
  getProjects,
  getRepoAdmins,
  putProjectConf,
  deleteProject,
  postProject,
  postAddRepoToProject, getTopCloneSources,
} from './services'

const setProjects = (payload): SetProjects => ({
  type: SET_PROJECTS,
  payload
})

const setProject = (payload): SetProject => ({
  type: SET_PROJECT,
  payload
})

const setTopPlagiarists = (payload): SetTopPlagiarists => ({
  type: SET_TOP_PLAGIARISTS,
  payload
})

const setTopCloneSources = (payload): SetTopCloneSources => ({
  type: SET_TOP_CLONE_SOURCES,
  payload
})

const setProjectConf = (payload): SetProjectConf => ({
  type: SET_PROJECT_CONF,
  payload
})

export const resetProjectInfo = (): ResetProjectInfo => ({
  type: RESET_PROJECT_INFO
})

export const resetProjectsAction = (): ResetProjects => ({
  type: RESET_PROJECTS
})

const setRepoAdmins = (payload): SetRepoAdmins => ({
  type: SET_REPO_ADMINS,
  payload
})

const setBaseFiles = (payload): SetBaseFiles => ({
  type: SET_BASE_FILES,
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
  try {
    dispatch(setProject(fetching))
    const project = await getProject(id)
    dispatch(setProject(fetched(project)))
  } catch (e) {
    dispatch(setProject(failed(e)))
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const getTopPlagiaristsAction = (projectId: number, handleError?: (msg: string) => void) => async (
    dispatch: AppDispatch, //
    getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (
      projects.topPlagiarists.isFetching ||
      (projects.topPlagiarists.value &&
          projects.topPlagiarists.value.length !== 0 &&
          projects.topPlagiarists.projectId === projectId)
  ) {
    return
  }
  try {
    dispatch(setTopPlagiarists(fetching))
    const result = await getTopPlagiarists(projectId)
    dispatch(setTopPlagiarists(fetched(result)))
  } catch (e) {
    dispatch(setTopPlagiarists(failed(e)))
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const getTopCloneSourcesAction = (projectId: number, handleError?: (msg: string) => void) => async (
    dispatch: AppDispatch, //
    getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (
      projects.topCloneSources.isFetching ||
      (projects.topCloneSources.value &&
          projects.topCloneSources.value.length !== 0 &&
          projects.topCloneSources.projectId === projectId)
  ) {
    return
  }
  try {
    dispatch(setTopCloneSources(fetching))
    const result = await getTopCloneSources(projectId)
    dispatch(setTopCloneSources(fetched(result)))
  } catch (e) {
    dispatch(setTopCloneSources(failed(e)))
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
  if (!users.token) {
    return
  }
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

export const getRepoAdminsAction = (
  projectId: number,
  handleError?: (msg: string) => void
) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.repoAdmins.value && projects.repoAdmins.projectId === projectId) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  if (!users.token) {
    return
  }
  try {
    dispatch(setRepoAdmins(fetching))
    const admins = await getRepoAdmins(projectId, users.token)
    dispatch(setRepoAdmins(fetched(admins, projectId)))
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
    case 'ALREADY_EXISTS':
      return 'Project for this repository is already exists!'
    case 'INVALID_URL':
      return 'URL to the repository is invalid!'
    case 'UNABLE_RETRIEVE_GITHUB_REPO':
      return 'Unable to retrieve specified github repo'
    case 'BAD_FORMAT':
      return 'Request body JSON object has bad format'
    default:
      return `Unknown error has occurred: ${error}`
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

export const addRepoToProjectAction = (
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
    const result = await postAddRepoToProject(projects.project.value.id, url, users.token)
    if (typeof result === 'string') {
      if (handleError) {
        handleError(messageFromError(result))
      }
    } else {
      dispatch(setProject(fetched(result)))
      if (handleSuccess) {
        handleSuccess()
      }
    }
  }
}

export const getBaseFilesAction = (
  projectId: number,
  handleError?: (msg: string) => void
) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { projects } = getState()
  if (projects.baseFiles.value && projects.baseFiles.projectId === projectId) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    dispatch(setBaseFiles(fetching))
    const files = await getBaseFiles(projectId, users.token)
    dispatch(setBaseFiles(fetched(files, projectId)))
  } catch (e) {
    dispatch(setBaseFiles(failed(e)))
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const deleteProjectAction = (
  id: number,
  handleSuccess?: () => void,
  handleError?: (msg: string) => void
) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    await deleteProject(id, users.token)
    if (handleSuccess) {
      handleSuccess()
    }
  } catch (e) {
    if (handleError) {
      handleError(e.message)
    }
  }
}
