import jwtDecode from 'jwt-decode'

import { AppDispatch, AppStateSupplier } from 'store'
import { IToken, IUser } from 'types'
import {
  FETCHING_USER,
  FetchingUser,
  SET_ACCESS_TOKEN,
  SET_USER,
  SetAccessToken,
  SetUser
} from './types'
import { getUserById, refreshToken } from './services'

export const setAccessTokenAction = (token: IToken): SetAccessToken => ({
  type: SET_ACCESS_TOKEN,
  token
})

const setUser = (user: IUser): SetUser => ({
  type: SET_USER,
  user
})

const fetchingUser = (isFetching: boolean): FetchingUser => ({
  type: FETCHING_USER,
  isFetching
})

export const getAccessTokenAction = () => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { users } = getState()
  if (users.token && users.token.accessToken) {
    const { exp } = jwtDecode(users.token.accessToken)
    const now = new Date().getTime() / 1000
    if (now <= exp) {
      return // token is valid
    }
  }
  try {
    const token = await refreshToken()
    dispatch(setAccessTokenAction(token))
  } catch (e) {
    console.log(e)
  }
}

export const requireToken = async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await getAccessTokenAction()(dispatch, getState)
}

export const getCurrentUserAction = () => async (
  dispatch: AppDispatch,
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users } = getState()
  if (users.token && users.token.accessToken) {
    const { sub } = jwtDecode(users.token.accessToken)
    try {
      dispatch(fetchingUser(true))
      const user = await getUserById(parseInt(sub, 10))
      dispatch(setUser(user))
    } catch (e) {
      console.log(e)
    } finally {
      dispatch(fetchingUser(false))
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
