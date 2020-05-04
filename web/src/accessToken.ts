import jwtDecode from 'jwt-decode'
import { refreshToken } from 'services/authService'

interface Token {
  accessToken: string
}

let accessToken = ''

const updateAccessToken = async () => {
  const token: Token = await refreshToken().catch(() => {
    throw Error('Refresh token failed')
  })
  accessToken = token.accessToken
  return accessToken
}

export const getAccessToken = async (): Promise<string> => {
  try {
    const now = new Date().getTime() / 1000
    const { exp } = jwtDecode(accessToken)
    if (now <= exp) {
      return accessToken
    }
    return updateAccessToken()
  } catch (Error) {
    return updateAccessToken()
  }
}

export const setAccessToken = (token: string) => {
  accessToken = token
}
