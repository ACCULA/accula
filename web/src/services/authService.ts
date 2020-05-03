import { API_URL } from 'utils'
import { get } from './shareService'

type AuthEndpoint = 'refreshToken' | 'logout'

const updateRefreshToken = (endpoint: AuthEndpoint) => {
  const options = {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    credentials: 'include'
  } as any
  return get(`${API_URL}/${endpoint}`, options)
}

export const refreshToken = () => updateRefreshToken('refreshToken')
export const removeRefreshToken = () => updateRefreshToken('logout')
