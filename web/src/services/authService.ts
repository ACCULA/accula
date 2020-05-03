import { API_URL } from 'utils'
import { get } from './shareService'

type AuthEndpoint = 'refreshToken' | 'logout'

const updateRefreshToken = (endpoint: AuthEndpoint) => {
  return get(`${API_URL}/${endpoint}`)
}

export const refreshToken = () => updateRefreshToken('refreshToken')
export const logout = () => updateRefreshToken('logout')
