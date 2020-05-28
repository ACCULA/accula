import axios from 'axios'

import { API_URL, DEBUG } from 'utils'
import { IClone, IDiff, IPull, IToken } from 'types'
import { pulls, clones } from 'stubs'

export const getPulls = async (token: IToken, projectId: number): Promise<IPull[]> => {
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
    .then(resp => resp.data as IPull[])
}

export const getPull = async (token: IToken, projectId: number, pullId: number): Promise<IPull> => {
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

export const getDiff = async (token: IToken, projectId: number, pullId: number): Promise<IDiff[]> => {
  if (DEBUG) {
    return Promise.resolve([])
  }
  return axios
    // TODO: remove sha
    .get(`${API_URL}/api/projects/${projectId}/pulls/${pullId}/diff?sha=0daef8b6940e974ca57c5afa647d30c87bfb61bd`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IDiff[])
}

export const getClones = async (token: IToken, projectId: number, pullId: number): Promise<IClone[]> => {
  if (true || DEBUG) {
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
