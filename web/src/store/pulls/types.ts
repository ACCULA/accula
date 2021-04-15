import { IClone, ICompareRef, IDiff, IPull, IPullRef, IShortPull } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PULLS = 'SET_PULLS'
export const SET_MY_PULLS = 'SET_MY_PULLS'
export const SET_PULL = 'SET_PULL'
export const SET_DIFFS = 'SET_DIFFS'
export const SET_COMPARES = 'SET_COMPARES'
export const SET_CLONES = 'SET_CLONES'
export const SET_COMPARE_WITH = 'SET_COMPARE_WITH'
export const RESET_PULLS_INFO = 'RESET_PULLS_INFO'
export const CLEAR_COMPARES = 'CLEAR_COMPARES'

export type IPullDiffsState = Wrapper<IDiff[]> & IPullRef
export type IPullComparesState = Wrapper<IDiff[]> & ICompareRef
export type IPullClonesState = Wrapper<IClone[]> & IPullRef

export interface PullsState {
  pulls: Wrapper<IShortPull[]>
  myPulls: Wrapper<IShortPull[]>
  pull: Wrapper<IPull>
  diffs: IPullDiffsState
  compares: IPullComparesState
  compareWith: number
  clones: IPullClonesState
}

export interface SetPulls {
  type: typeof SET_PULLS
  payload: Wrapper<IShortPull[]>
}

export interface SetMyPulls {
  type: typeof SET_MY_PULLS
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

export interface SetCompareWith {
  type: typeof SET_COMPARE_WITH
  payload: number
}

export interface SetClones {
  type: typeof SET_CLONES
  payload: Wrapper<IClone[]> & IPullRef
}

export interface ResetPullsInfo {
  type: typeof RESET_PULLS_INFO
}
export interface ClearCompares {
  type: typeof CLEAR_COMPARES
}
export type PullsActionTypes =
  | SetPulls //
  | SetMyPulls
  | SetPull
  | SetDiffs
  | SetCompares
  | SetClones
  | ResetPullsInfo
  | SetCompareWith
  | ClearCompares
