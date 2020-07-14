import axios from 'axios'

import { API_URL, DEBUG } from 'utils'
import { IClone, IDiff, IPull, IShortPull, IToken } from 'types'
import { pulls, clones } from 'stubs'

export const getPulls = async (
  token: IToken, //
  projectId: number
): Promise<IShortPull[]> => {
  if (DEBUG) {
    return Promise.resolve(pulls)
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IShortPull[])
}

export const getPull = async (
  token: IToken, //
  projectId: number,
  pullId: number
): Promise<IPull> => {
  if (DEBUG) {
    return Promise.resolve(pulls.find(p => p.number === pullId))
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IPull)
}

export const getDiffs = async (
  token: IToken,
  projectId: number,
  pullId: number
): Promise<IDiff[]> => {
  if (DEBUG) {
    return Promise.resolve([])
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}/diff`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IDiff[])
}

export const getCompares = async (
  token: IToken,
  projectId: number,
  target: number,
  source: number
): Promise<IDiff[]> => {
  if (DEBUG) {
    return Promise.resolve([])
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/diff?source=${source}&target=${target}`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
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
  if (DEBUG) {
    return Promise.resolve(clones)
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}/clones`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
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
  if (DEBUG) {
    return Promise.resolve([])
  }
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
