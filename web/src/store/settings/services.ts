import axios from 'axios'
import { IAppSettings, IToken } from '../../types'
import { API_URL } from '../../utils'

export const getAppSettings = async (token: IToken): Promise<IAppSettings> => {
  return axios
    .get(`${API_URL}/api/app/settings`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IAppSettings)
}

export const putAppSettings = async (
  token: IToken,
  settings: IAppSettings
): Promise<IAppSettings> => {
  return axios
    .put(`${API_URL}/api/app/settings`, settings, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token.accessToken}`
      },
      withCredentials: true
    })
    .then(resp => resp.data as IAppSettings)
}
