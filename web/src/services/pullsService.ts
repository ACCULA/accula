import axios from 'axios'

import { API_URL } from 'utils'
import { IPull } from 'types'
import { pulls } from 'data'

const debug = true

export const getPulls = async (projectId: number): Promise<IPull[]> => {
  if (debug) {
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
  if (debug) {
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
