import {
  FETCHING_PULLS,
  PullsActionTypes,
  PullsState,
  SET_CLONES,
  SET_DIFFS,
  SET_PULL,
  SET_PULLS
} from 'store/pulls/types'

const initialState: PullsState = {
  pulls: null,
  pull: null,
  clones: null,
  diffs: null,
  isFetching: false
}

export function pullsReducer(
  state = initialState, //
  action: PullsActionTypes
): PullsState {
  switch (action.type) {
    case SET_PULLS: {
      return {
        ...state,
        pulls: action.pulls
      }
    }
    case SET_PULL: {
      return {
        ...state,
        pull: action.pull
      }
    }
    case SET_CLONES: {
      return {
        ...state,
        clones: action.clones
      }
    }
    case SET_DIFFS: {
      return {
        ...state,
        diffs: action.diffs.map(diff => ({
          baseFilename: diff.baseFilename,
          baseContent: diff.baseContent ? atob(diff.baseContent) : '',
          headFilename: diff.headFilename,
          headContent: diff.headContent ? atob(diff.headContent) : ''
        }))
      }
    }
    case FETCHING_PULLS: {
      return {
        ...state,
        isFetching: action.isFetching
      }
    }
    default:
      return state
  }
}
