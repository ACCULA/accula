import { IClone, IDiff, IPull } from 'types'

export const SET_PULLS = 'SET_PULLS'
export const SET_PULL = 'SET_PULL'
export const SET_DIFFS = 'SET_DIFF'
export const SET_CLONES = 'SET_CLONES'
export const FETCHING_PULLS = 'FETCHING_PULLS'

export interface PullsState {
  pulls?: IPull[]
  pull?: IPull
  clones?: IClone[]
  diffs?: IDiff[]
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

export interface SetDiffs {
  type: typeof SET_DIFFS
  diffs: IDiff[]
}

export interface SetClones {
  type: typeof SET_CLONES
  clones: IClone[]
}

export interface FetchingPulls {
  type: typeof FETCHING_PULLS
  isFetching: boolean
}

export type PullsActionTypes =
  | SetPulls //
  | SetPull
  | SetClones
  | SetDiffs
  | FetchingPulls
