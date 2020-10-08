import { IClone, ICompareRef, IDiff, IPull, IPullRef, IShortPull } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PULLS = 'SET_PULLS'
export const SET_PULL = 'SET_PULL'
export const SET_DIFFS = 'SET_DIFFS'
export const SET_COMPARES = 'SET_COMPARES'
export const SET_CLONES = 'SET_CLONES'
export const RESET_PULLS_INFO = 'RESET_PULLS_INFO'

export type IPullDiffsState = Wrapper<IDiff[]> & IPullRef
export type IPullComparesState = Wrapper<IDiff[]> & ICompareRef
export type IPullClonesState = Wrapper<IClone[]> & IPullRef

export interface PullsState {
  pulls: Wrapper<IShortPull[]>
  pull: Wrapper<IPull>
  diffs: IPullDiffsState
  compares: IPullComparesState
  clones: IPullClonesState
}

export interface SetPulls {
  type: typeof SET_PULLS
  payload: Wrapper<IShortPull[]>
}

export interface SetPull {
  type: typeof SET_PULL
  payload: Wrapper<IPull>
}

export interface SetDiffs {
  type: typeof SET_DIFFS
  payload: Wrapper<IDiff[]> & IPullRef
}

export interface SetCompares {
  type: typeof SET_COMPARES
  payload: Wrapper<IDiff[]> & IPullRef
}

export interface SetClones {
  type: typeof SET_CLONES
  payload: Wrapper<IClone[]> & IPullRef
}

export interface ResetPullsInfo {
  type: typeof RESET_PULLS_INFO
}

export type PullsActionTypes =
  | SetPulls //
  | SetPull
  | SetDiffs
  | SetCompares
  | SetClones
  | ResetPullsInfo
