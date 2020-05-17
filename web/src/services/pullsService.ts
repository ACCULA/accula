import axios from 'axios'

import { API_URL, DEBUG } from 'utils'
import { IPull } from 'types'
import { pulls } from 'data'

export const getPulls = async (projectId: number): Promise<IPull[]> => {
  if (DEBUG) {
    return Promise.resolve(pulls)
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IPull[])
}

export const getPull = async (projectId: number, id: number): Promise<IPull> => {
  if (DEBUG) {
    return Promise.resolve(pulls.find(p => p.id === id))
  }
  return axios
    .get(`${API_URL}/api/projects/${projectId}/pulls/${id}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IPull)
}
