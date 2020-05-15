import axios from 'axios'

import { API_URL } from 'utils'
import { Token } from 'types'

const REFRESH_TOKEN = 'refreshToken'
const LOGOUT = 'logout'

type AuthEndpoint =
  | typeof REFRESH_TOKEN //
  | typeof LOGOUT

const updateRefreshToken = (endpoint: AuthEndpoint): Promise<Token> => {
  return axios
    .get(`${API_URL}/api/${endpoint}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as Token)
}

export const refreshToken = () => updateRefreshToken(REFRESH_TOKEN)
export const logout = () => updateRefreshToken(LOGOUT)
