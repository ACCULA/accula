import axios from 'axios'

import { API_URL } from 'utils'
import { IClone, IDiff, IPull, IShortPull, IToken } from 'types'

export const getPulls = async (token: IToken, projectId: number, assignedToMe): Promise<IShortPull[]> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true,
      params: assignedToMe ? {
        assignedToMe: true
      } : {}
    })
    .then(resp => resp.data as IShortPull[])
}

export const getPull = async (projectId: number, pullId: number): Promise<IPull> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IPull)
}

export const getDiffs = async (projectId: number, pullId: number): Promise<IDiff[]> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}/diff`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IDiff[])
}

export const getCompares = async (
  projectId: number,
  target: number,
  source: number
): Promise<IDiff[]> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${source}/compare?with=${target}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IDiff[])
}

export const getClones = async (
  token: IToken,
  projectId: number,
  pullId: number
): Promise<IClone[]> => {
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}/clones`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IClone[])
}

export const refreshClones = async (
  token: IToken,
  projectId: number,
  pullId: number
): Promise<IClone[]> => {
  return axios //
    .post(`${API_URL}/api/projects/${projectId}/pulls/${pullId}/clones/refresh`, null, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IClone[])
}
