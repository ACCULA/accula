import { notFetching, Wrapper } from 'store/wrapper'
import { IClone, IDiff } from 'types'
import { decode } from 'js-base64'
import {
  PullsActionTypes, //
  PullsState,
  SET_CLONES,
  SET_MY_PULLS,
  SET_COMPARES,
  SET_DIFFS,
  SET_PULL,
  SET_PULLS,
  RESET_PULLS_INFO,
  SET_COMPARE_WITH,
  CLEAR_COMPARES
} from './types'

const decodeDiff = (wrapper: Wrapper<IDiff[]>): Wrapper<IDiff[]> => ({
  ...wrapper,
  value:
    wrapper.value &&
    wrapper.value.map(diff => ({
      baseFilename: diff.baseFilename,
      baseContent: diff.baseContent ? decode(diff.baseContent) : '',
      headFilename: diff.headFilename,
      headContent: diff.headContent ? decode(diff.headContent) : ''
    }))
})

const decodeClones = (wrapper: Wrapper<IClone[]>): Wrapper<IClone[]> => ({
  ...wrapper,
  value:
    wrapper.value &&
    wrapper.value.map(diff => ({
      id: diff.id,
      projectId: diff.projectId,
      target: {
        ...diff.target,
        content: decode(diff.target.content)
      },
      source: {
        ...diff.source,
        content: decode(diff.source.content)
      }
    }))
})

const initialState: PullsState = {
  pulls: notFetching,
  myPulls: notFetching,
  pull: notFetching,
  diffs: notFetching,
  compares: notFetching,
  compareWith: undefined,
  clones: notFetching
}

export function pullsReducer(
  state: PullsState = initialState, //
  action: PullsActionTypes
): PullsState {
  switch (action.type) {
    case SET_PULLS: {
      return {
        ...state,
        pulls: action.payload
      }
    }
    case SET_MY_PULLS: {
      return {
        ...state,
        myPulls: action.payload
      }
    }
    case SET_PULL: {
      return {
        ...state,
        pull: action.payload
      }
    }
    case SET_CLONES: {
      return {
        ...state,
        clones: decodeClones(action.payload)
      }
    }
    case SET_DIFFS: {
      return {
        ...state,
        diffs: decodeDiff(action.payload)
      }
    }
    case SET_COMPARES: {
      return {
        ...state,
        compares: decodeDiff(action.payload)
      }
    }
    case SET_COMPARE_WITH: {
      return {
        ...state,
        compareWith: action.payload
      }
    }
    case CLEAR_COMPARES: {
      return {
        ...state,
        compares: notFetching,
        compareWith: undefined
      }
    }
    case RESET_PULLS_INFO: {
      return initialState
    }
    default:
      return state
  }
}
