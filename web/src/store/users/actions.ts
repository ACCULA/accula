import jwtDecode from 'jwt-decode'

import { AppDispatch, AppState } from 'store'
import {
  FETCHING_USER,
  FetchingUser,
  SET_ACCESS_TOKEN,
  SET_USER,
  SetAccessToken,
  SetUser
} from 'store/users/types'
import { refreshToken } from 'services/authService'
import { getUserById } from 'services/userService'
import { Token, User } from 'types'

export const setAccessTokenAction = (token: Token): SetAccessToken => ({
  type: SET_ACCESS_TOKEN,
  token
})

const setUser = (user: User): SetUser => ({
  type: SET_USER,
  user
})

const fetchingUser = (isFetching: boolean): FetchingUser => ({
  type: FETCHING_USER,
  isFetching
})

export const getAccessTokenAction = () => async (
  dispatch: AppDispatch, //
  getState: () => AppState
) => {
  const { users } = getState()
  if (!users.token) {
    try {
      dispatch(fetchingUser(true))
      const token = await refreshToken()
      dispatch(setAccessTokenAction(token))
    } catch (e) {
      console.log(e)
    } finally {
      dispatch(fetchingUser(false))
    }
  }
}

export const getCurrentUserAction = () => async (
  dispatch: AppDispatch,
  getState: () => AppState
) => {
  const { users } = getState()
  if (users.token && users.token.accessToken) {
    const { sub, exp } = jwtDecode(users.token.accessToken)
    const now = new Date().getTime() / 1000
    if (now <= exp) {
      try {
        dispatch(fetchingUser(true))
        const user = await getUserById(parseInt(sub, 10))
        dispatch(setUser(user))
      } catch (e) {
        console.log(e)
      } finally {
        dispatch(fetchingUser(false))
      }
    } else {
      dispatch(setAccessTokenAction(null))
    }
  }
}

export const getUserAction = (id: number) => async (
  dispatch: AppDispatch //
) => {
  try {
    dispatch(fetchingUser(true))
    const user = await getUserById(id)
    dispatch(setUser(user))
  } catch (e) {
    console.log(e)
  } finally {
    dispatch(fetchingUser(false))
  }
}
