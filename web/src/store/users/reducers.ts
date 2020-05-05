import { GET_USER, UsersActionTypes, UsersState } from 'store/users/types'

const initialState: UsersState = {
  user: null,
  isFetching: false
}

export function usersReducer(
  state = initialState,
  action: UsersActionTypes
): UsersState {
  switch (action.type) {
    case GET_USER:
      return {
        ...state,
        user: action.user,
        isFetching: false
      }
    default:
      return state
  }
}
