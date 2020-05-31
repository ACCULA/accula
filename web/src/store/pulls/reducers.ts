import { notFetching } from 'store/wrapper'
import {
  PullsActionTypes, //
  PullsState,
  SET_CLONES,
  SET_DIFFS,
  SET_PULL,
  SET_PULLS
} from './types'

const initialState: PullsState = {
  pulls: notFetching,
  pull: notFetching,
  clones: notFetching,
  diff: notFetching
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
    case SET_PULL: {
      return {
        ...state,
        pull: action.payload
      }
    }
    case SET_CLONES: {
      return {
        ...state,
        clones: {
          ...action.payload,
          value:
            action.payload.value &&
            action.payload.value.map(diff => ({
              id: diff.id,
              target: {
                ...diff.target,
                content: atob(diff.target.content)
              },
              source: {
                ...diff.source,
                content: atob(diff.source.content)
              }
            }))
        }
      }
    }
    case SET_DIFFS: {
      return {
        ...state,
        diff: {
          ...action.payload,
          value:
            action.payload.value &&
            action.payload.value.map(diff => ({
              baseFilename: diff.baseFilename,
              baseContent: diff.baseContent ? atob(diff.baseContent) : '',
              headFilename: diff.headFilename,
              headContent: diff.headContent ? atob(diff.headContent) : ''
            }))
        }
      }
    }
    default:
      return state
  }
}
