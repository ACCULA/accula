import { FETCHING_USER, GET_USER, GetUser } from 'store/users/types'
import { getUserById } from 'services/userService'
import { AppDispatch } from 'store/index'
import { User } from 'types'

const fetchingUser = () => ({
  type: FETCHING_USER
})

const getUser = (user: User): GetUser => ({
  type: GET_USER,
  user
})

export const getUserAction = (id: number): ((dispatch: AppDispatch) => Promise<void>) => async (
  dispatch: AppDispatch
) => {
  try {
    dispatch(fetchingUser())
    const response = await getUserById(id)
    dispatch(getUser(response))
  } catch (err) {
    console.log(err)
  }
}
