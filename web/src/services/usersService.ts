import axios from 'axios'

import { API_URL } from 'utils'
import { IUser } from 'types'

export const getUserById = async (id: number): Promise<IUser> => {
  return axios
    .get(`${API_URL}/api/users/${id}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IUser)
}
