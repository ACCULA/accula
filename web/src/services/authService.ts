import axios from 'axios'
import { API_URL } from 'utils'

type AuthEndpoint = 'refreshToken' | 'logout'

const updateRefreshToken = (endpoint: AuthEndpoint) => {
  return axios
    .get(`${API_URL}/${endpoint}`, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data)
}

export const refreshToken = () => updateRefreshToken('refreshToken')
export const logout = () => updateRefreshToken('logout')
