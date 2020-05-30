import { IClone, IDiff, IPull, IPullRef } from 'types'
import { Wrapper } from 'store/wrapper'

export const SET_PULLS = 'SET_PULLS'
export const SET_PULL = 'SET_PULL'
export const SET_DIFFS = 'SET_DIFF'
export const SET_CLONES = 'SET_CLONES'

export interface PullsState {
  pulls: Wrapper<IPull[]>
  pull: Wrapper<IPull>
  diff: Wrapper<IDiff[]> & IPullRef
  clones: Wrapper<IClone[]> & IPullRef
}

export interface SetPulls {
  type: typeof SET_PULLS
  payload: Wrapper<IPull[]>
}

export interface SetPull {
  type: typeof SET_PULL
  payload: Wrapper<IPull>
}

export interface SetDiffs {
  type: typeof SET_DIFFS
  payload: Wrapper<IDiff[]> & IPullRef
}

export interface SetClones {
  type: typeof SET_CLONES
  payload: Wrapper<IClone[]> & IPullRef
}

export type PullsActionTypes =
  | SetPulls //
  | SetPull
  | SetClones
  | SetDiffs
