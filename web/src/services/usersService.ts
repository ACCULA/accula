import axios from 'axios'

import { API_URL } from 'utils'
import { User } from 'types'

export const getUserById = async (id: number): Promise<User> => {
  return axios
    .get(`${API_URL}/users/${id}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as User)
}
