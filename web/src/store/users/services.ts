import axios from 'axios'

import { API_URL } from 'utils'
import { IUser, IToken } from 'types'

const REFRESH_TOKEN = 'refreshToken'
const LOGOUT = 'logout'

type AuthEndpoint =
  | typeof REFRESH_TOKEN //
  | typeof LOGOUT

const updateRefreshToken = (endpoint: AuthEndpoint): Promise<IToken> => {
  return axios
    .get(`${API_URL}/api/${endpoint}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data as IToken)
}

export const refreshToken = () => updateRefreshToken(REFRESH_TOKEN)
export const logout = () => updateRefreshToken(LOGOUT)

export const getUserById = async (id: number, token: IToken): Promise<IUser> => {
  return axios
    .get(`${API_URL}/api/users/${id}`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IUser)
}
