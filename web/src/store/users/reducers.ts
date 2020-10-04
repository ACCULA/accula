import {
  SET_ACCESS_TOKEN, //
  SET_USER,
  CHANGE_SETTINGS,
  UsersActionTypes,
  UsersState
} from './types'

const initialState: UsersState = {
  user: { isFetching: null },
  token: {
    accessToken: null
  },
  settings: {
    themeMode: 'light'
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
    case CHANGE_SETTINGS: {
      return {
        ...state,
        settings: action.settings
      }
    }
    default:
      return state
  }
}
