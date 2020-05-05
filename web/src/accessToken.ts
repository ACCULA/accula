import jwtDecode from 'jwt-decode'
import { refreshToken } from 'services/authService'

let accessToken = ''

const updateAccessToken = async () => {
  const token = await refreshToken().catch(() => {
    console.error('Refresh token failed')
    return {
      accessToken: ''
    }
  })
  accessToken = token.accessToken
  return accessToken
}

export const getAccessToken = async (): Promise<string> => {
  try {
    if (accessToken && accessToken !== '') {
      const now = new Date().getTime() / 1000
      const { exp } = jwtDecode(accessToken)
      if (now <= exp) {
        return accessToken
      }
    }
    return updateAccessToken()
  } catch (e) {
    return updateAccessToken()
  }
}

export const setAccessToken = (token: string) => {
  accessToken = token
}
