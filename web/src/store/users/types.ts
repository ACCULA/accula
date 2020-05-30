import { IToken, IUser } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_ACCESS_TOKEN = 'SET_ACCESS_TOKEN'
export const SET_USER = 'SET_USER'

export interface UsersState {
  user: Wrapper<IUser>
  token: IToken
}

export interface SetAccessToken {
  type: typeof SET_ACCESS_TOKEN
  token: IToken
}

export interface SetUser {
  type: typeof SET_USER
  payload: Wrapper<IUser>
}

export type UsersActionTypes =
  | SetAccessToken //
  | SetUser
