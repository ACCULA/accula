import { User } from 'types'

export const GET_USER = 'GET_USER'
export const FETCHING_USER = 'FETCHING_USER'

export interface UsersState {
  user: User
  isFetching: boolean
}

export interface GetUser {
  type: typeof GET_USER
  user: User
}

export interface FetchingUser {
  type: typeof FETCHING_USER
}

export type UsersActionTypes = GetUser | FetchingUser
