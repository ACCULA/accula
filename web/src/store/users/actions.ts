import { FETCHING_USER, FetchingUser, GET_USER, GetUser } from 'store/users/types'
import { getUserById } from 'services/userService'
import { AppDispatch } from 'store/index'
import { User } from 'types'

const getUser = (user: User): GetUser => ({
  type: GET_USER,
  user
})

const fetchingUser = (isFetching: boolean): FetchingUser => ({
  type: FETCHING_USER,
  isFetching
})

export const getUserAction = (id: number): ((dispatch: AppDispatch) => Promise<void>) => async (
  dispatch: AppDispatch
) => {
  try {
    dispatch(fetchingUser(true))
    const user = await getUserById(id)
    dispatch(getUser(user))
  } catch (error) {
    console.log(error)
  } finally {
    dispatch(fetchingUser(false))
  }
}
