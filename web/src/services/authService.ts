import axios from 'axios'

import { API_URL } from 'utils'

type AuthEndpoint = 'refreshToken' | 'logout'

interface Token {
  accessToken: string
}

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
