import axios from 'axios'

import { API_URL } from 'utils'
import { Project } from 'types'
import { projects } from 'data'

const debug = true

export const getProjects = async (): Promise<Project[]> => {
  // TODO: temp
  if (debug) {
    return Promise.resolve(projects)
  }
  return axios
    .get(`${API_URL}/projects`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as Project[])
}
