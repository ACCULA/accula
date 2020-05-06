import axios from 'axios'

import { API_URL } from 'utils'
import { Token } from 'types'

type AuthEndpoint = 'refreshToken' | 'logout'

const updateRefreshToken = (endpoint: AuthEndpoint): Promise<Token> => {
  return axios
    .get(`${API_URL}/${endpoint}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as Token)
}

export const refreshToken = () => updateRefreshToken('refreshToken')
export const logout = () => updateRefreshToken('logout')
