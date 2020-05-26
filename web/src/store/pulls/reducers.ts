import { FETCHING_PULLS, PullsActionTypes, PullsState, SET_CLONES, SET_PULL, SET_PULLS } from 'store/pulls/types'

const initialState: PullsState = {
  pulls: null,
  pull: null,
  clones: null,
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
