import { IPull } from 'types'

export const SET_PULLS = 'SET_PULLS'
export const SET_PULL = 'SET_PULL'
export const FETCHING_PULLS = 'FETCHING_PULLS'

export interface PullsState {
  pulls?: IPull[]
  pull?: IPull
  isFetching: boolean
}

export interface SetPulls {
  type: typeof SET_PULLS
  pulls: IPull[]
}

export interface SetPull {
  type: typeof SET_PULL
  pull: IPull
}

export interface FetchingPulls {
  type: typeof FETCHING_PULLS
  isFetching: boolean
}

export type PullsActionTypes =
  | SetPulls //
  | SetPull
  | FetchingPulls
