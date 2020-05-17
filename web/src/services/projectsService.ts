import axios from 'axios'

import { API_URL } from 'utils'
import { IProject } from 'types'
import { projects } from 'data'

const debug = false

export const getProjects = async (): Promise<IProject[]> => {
  if (debug) {
    return Promise.resolve(projects)
  }
  return axios
    .get(`${API_URL}/api/projects`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProject[])
}

export const getProject = async (id: number): Promise<IProject> => {
  if (debug) {
    return Promise.resolve(projects[0])
  }
  return axios
    .get(`${API_URL}/api/projects/${id}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IProject)
}

export const createProject = async (url: string, token: String): Promise<IProject> => {
  if (debug) {
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
          Authorization: `Bearer ${token}`
        },
        withCredentials: true
      }
    )
    .then(resp => resp.data as IProject)
}
