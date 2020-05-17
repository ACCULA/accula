import { IToken, IUser } from 'types'

export const SET_ACCESS_TOKEN = 'SET_ACCESS_TOKEN'
export const SET_USER = 'GET_USER'
export const FETCHING_USER = 'FETCHING_USER'

export interface UsersState {
  user?: IUser
  token?: IToken
  isFetching: boolean
}

export interface SetAccessToken {
  type: typeof SET_ACCESS_TOKEN
  token: IToken
}

export interface SetUser {
  type: typeof SET_USER
  user: IUser
}

export interface FetchingUser {
  type: typeof FETCHING_USER
  isFetching: boolean
}

export type UsersActionTypes =
  | SetAccessToken //
  | SetUser
  | FetchingUser
