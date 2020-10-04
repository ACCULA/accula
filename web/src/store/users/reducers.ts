import {
  SET_ACCESS_TOKEN, //
  SET_USER,
  UsersActionTypes,
  UsersState
} from './types'

const initialState: UsersState = {
  user: { isFetching: null },
  token: {
    accessToken: null
  }
}

export function usersReducer(
  state: UsersState = initialState, //
  action: UsersActionTypes
): UsersState {
  switch (action.type) {
    case SET_USER:
      return {
        ...state,
        user: action.payload
      }
    case SET_ACCESS_TOKEN: {
      return {
        ...state,
        token: action.token
      }
    }

    default:
      return state
  }
}
