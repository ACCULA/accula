import {
  FETCHING_USER,
  SET_USER,
  SET_ACCESS_TOKEN,
  UsersActionTypes,
  UsersState
} from 'store/users/types'

const initialState: UsersState = {
  user: null,
  isFetching: false
}

export function usersReducer(
  state = initialState,
  action: UsersActionTypes
): UsersState {
  switch (action.type) {
    case SET_ACCESS_TOKEN: {
      return {
        ...state,
        token: action.token
      }
    }
    case SET_USER:
      return {
        ...state,
        user: action.user
      }
    case FETCHING_USER:
      return {
        ...state,
        isFetching: action.isFetching
      }
    default:
      return state
  }
}
