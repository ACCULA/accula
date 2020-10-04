import { IToken, IUser, ISettings } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_ACCESS_TOKEN = 'SET_ACCESS_TOKEN'
export const SET_USER = 'SET_USER'
export const CHANGE_SETTINGS = 'CHANGE_SETTINGS'

export interface UsersState {
  user: Wrapper<IUser>
  token: IToken
  settings: ISettings
}

export interface SetAccessToken {
  type: typeof SET_ACCESS_TOKEN
  token: IToken
}

export interface SetUser {
  type: typeof SET_USER
  payload: Wrapper<IUser>
}

export interface ChangeSettings {
  type: typeof CHANGE_SETTINGS
  settings: ISettings
}

export type UsersActionTypes =
  | SetAccessToken //
  | SetUser
  | ChangeSettings
