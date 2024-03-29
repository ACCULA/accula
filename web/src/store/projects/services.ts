import axios from 'axios'

import { API_URL } from 'utils'
import { ICloneStatistics, IProject, IProjectConf, IToken } from 'types'

export const getProjects = async (): Promise<IProject[]> => {
  return axios
    .get(`${API_URL}/api/projects?count=100`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProject[])
}

export const getProject = async (id: number): Promise<IProject> => {
  return axios
    .get(`${API_URL}/api/projects/${id}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProject)
}

export const getTopPlagiarists = async (projectId: number): Promise<ICloneStatistics[]> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/topPlagiarists`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as ICloneStatistics[])
}

export const getTopCloneSources = async (projectId: number): Promise<ICloneStatistics[]> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/topCloneSources`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as ICloneStatistics[])
}

export const getProjectConf = async (id: number, token: IToken): Promise<IProjectConf> => {
  return axios
    .get(`${API_URL}/api/projects/${id}/conf`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProjectConf)
}

export const putProjectConf = async (
  id: number,
  token: IToken,
  conf: IProjectConf
): Promise<void> => {
  return axios //
    .put(`${API_URL}/api/projects/${id}/conf`, conf, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
}

export const postProject = async (url: string, token: IToken): Promise<IProject | string> => {
  return axios
    .post(
      `${API_URL}/api/projects`,
      {
        githubRepoUrl: url
      },
      {
        headers: {
          Accept: 'application/json',
          Authorization: `Bearer ${token.accessToken}`
        },
        withCredentials: true
      }
    )
    .then(resp => resp.data as IProject)
    .catch(rej => rej.response.data?.code || 'UNKNOWN_ERROR')
}

export const deleteProject = async (id: number, token: IToken): Promise<void> => {
  return axios //
    .delete(`${API_URL}/api/projects/${id}`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
}

export const postAddRepoToProject = async (
  id: number,
  url: string,
  token: IToken
): Promise<IProject | string> => {
  return axios
    .post(
      `${API_URL}/api/projects/${id}/addRepoByUrl`,
      {
        url
      },
      {
        headers: {
          Accept: 'application/json',
          Authorization: `Bearer ${token.accessToken}`
        },
        withCredentials: true
      }
    )
    .then(resp => resp.data as IProject)
    .catch(rej => rej.response.data?.code || 'UNKNOWN_ERROR')
}
