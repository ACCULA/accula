import axios from 'axios'

import { API_URL, DEBUG } from 'utils'
import { IProject, IToken, IUser } from 'types'
import { projects } from 'stubs'

export const getProjects = async (): Promise<IProject[]> => {
  if (DEBUG) {
    return Promise.resolve(projects)
  }
  return axios
    .get(`${API_URL}/api/projects?count=100`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProject[])
}

export const getProject = async (id: number, token: IToken): Promise<IProject> => {
  if (DEBUG) {
    return Promise.resolve(projects.find(p => p.id === id))
  }
  return axios
    .get(`${API_URL}/api/projects/${id}`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProject)
}

export const getRepoAdmins = async (id: number, token: IToken): Promise<IUser[]> => {
  if (DEBUG) {
    return Promise.resolve([])
  }
  return axios
    .get(`${API_URL}/api/projects/${id}/admins`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IUser[])
}

export const createProject = async (url: string, token: IToken): Promise<IProject | string> => {
  if (DEBUG) {
    return Promise.resolve(projects[0])
  }
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
    .catch(rej => rej.response.data?.error || 'UNKNOWN_ERROR')
}
