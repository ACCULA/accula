import jwtDecode from 'jwt-decode'

import { AppDispatch, AppStateSupplier } from 'store'
import { IToken } from 'types'
import { failed, fetched, fetching } from 'store/wrapper'
import { SET_ACCESS_TOKEN, SET_USER, SetAccessToken, SetUser } from './types'
import { getUserById, refreshToken } from './services'

export const setAccessTokenAction = (token: IToken): SetAccessToken => ({
  type: SET_ACCESS_TOKEN,
  token
})

const setUser = (payload): SetUser => ({
  type: SET_USER,
  payload
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
  if (!users.token.accessToken) {
    return
  }
  if (users.user.isFetching) {
    return
  }
  try {
    const { sub } = jwtDecode(users.token.accessToken)
    const id = parseInt(sub, 10)
    if (users.user.value && users.user.value.id === id) {
      return
    }
    dispatch(setUser(fetching))
    const user = await getUserById(id)
    dispatch(setUser(fetched(user)))
  } catch (e) {
    dispatch(setUser(failed(e)))
  }
}
