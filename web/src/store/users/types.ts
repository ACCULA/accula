import { Token, User } from 'types'

export const SET_ACCESS_TOKEN = 'SET_ACCESS_TOKEN'
export const SET_USER = 'GET_USER'
export const FETCHING_USER = 'FETCHING_USER'

export interface UsersState {
  user?: User
  token?: Token
  isFetching: boolean
}

export interface SetAccessToken {
  type: typeof SET_ACCESS_TOKEN
  token: Token
}

export interface SetUser {
  type: typeof SET_USER
  user: User
}

export interface FetchingUser {
  type: typeof FETCHING_USER
  isFetching: boolean
}

export type UsersActionTypes =
  | SetAccessToken //
  | SetUser
  | FetchingUser
