import axios from 'axios'

import { API_URL } from 'utils'
import { getAccessToken } from 'accessToken'
import { User } from 'types'

export const getUserById = async (id: number): Promise<User> => {
  const accessToken = await getAccessToken()
  return axios
    .get(`${API_URL}/users/${id}`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as User)
}
