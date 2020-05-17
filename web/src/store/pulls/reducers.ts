import { PullsActionTypes, PullsState, SET_PULL, SET_PULLS } from 'store/pulls/types'

const initialState: PullsState = {
  pulls: null,
  pull: null,
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
    default:
      return state
  }
}
