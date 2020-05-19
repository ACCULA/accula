import { AppDispatch, AppStateSupplier } from 'store'
import { IPull } from 'types'
import {
  FETCHING_PULLS, //
  FetchingPulls,
  SET_PULL,
  SET_PULLS,
  SetPull,
  SetPulls
} from './types'
import { getPull, getPulls } from './services'

const setPulls = (pulls: IPull[]): SetPulls => ({
  type: SET_PULLS,
  pulls
})

const setPull = (pull: IPull): SetPull => ({
  type: SET_PULL,
  pull
})

const fetchingPulls = (isFetching: boolean): FetchingPulls => ({
  type: FETCHING_PULLS,
  isFetching
})

export const getPullsAction = (projectId: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { pulls } = getState()
  if (!pulls.pulls || (pulls.pulls.length > 0 && pulls.pulls[0].projectId !== projectId)) {
    try {
      dispatch(fetchingPulls(true))
      const result = await getPulls(projectId)
      dispatch(setPulls(result))
    } catch (e) {
      console.log(e)
      dispatch(setPulls([]))
    } finally {
      dispatch(fetchingPulls(false))
    }
  }
}

export const getPullAction = (projectId: number, id: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { pulls } = getState()
  if (!pulls.pull || pulls.pull.projectId !== projectId || pulls.pull.id !== id) {
    try {
      dispatch(fetchingPulls(true))
      const pull = await getPull(projectId, id)
      dispatch(setPull(pull))
    } catch (e) {
      console.log(e)
      dispatch(setPull(null))
    } finally {
      dispatch(fetchingPulls(false))
    }
  }
}
