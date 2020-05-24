import { AppDispatch, AppStateSupplier } from 'store'
import { IClone, IPull } from 'types'
import {
  FETCHING_PULLS,
  FetchingPulls,
  SET_CLONES,
  SET_PULL,
  SET_PULLS,
  SetClones,
  SetPull,
  SetPulls
} from './types'
import { getClones, getPull, getPulls } from './services'

const setPulls = (pulls: IPull[]): SetPulls => ({
  type: SET_PULLS,
  pulls
})

const setPull = (pull: IPull): SetPull => ({
  type: SET_PULL,
  pull
})

const setClones = (clones: IClone[]): SetClones => ({
  type: SET_CLONES,
  clones
})

const fetchingPulls = (isFetching: boolean): FetchingPulls => ({
  type: FETCHING_PULLS,
  isFetching
})

export const getPullsAction = (projectId: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { pulls, users } = getState()
  if (!users.token || pulls.isFetching) {
    return
  }
  if (!pulls.pulls || (pulls.pulls.length > 0 && pulls.pulls[0].projectId !== projectId)) {
    try {
      dispatch(fetchingPulls(true))
      const result = await getPulls(users.token, projectId)
      dispatch(setPulls(result))
    } catch (e) {
      console.log(e)
      dispatch(setPulls([]))
    } finally {
      dispatch(fetchingPulls(false))
    }
  }
}

export const getPullAction = (projectId: number, pullId: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { pulls, users } = getState()
  if (!users.token || pulls.isFetching) {
    return
  }
  if (pulls.pulls) {
    const pull = pulls.pulls.find(p => p.number === pullId && p.projectId === projectId)
    if (pull) {
      dispatch(setPull(pull))
      return
    }
  }
  if (!pulls.pull || pulls.pull.projectId !== projectId || pulls.pull.number !== pullId) {
    try {
      dispatch(fetchingPulls(true))
      const pull = await getPull(users.token, projectId, pullId)
      dispatch(setPull(pull))
    } catch (e) {
      console.log(e)
      dispatch(setPull(null))
    } finally {
      dispatch(fetchingPulls(false))
    }
  }
}

export const getClonesAction = (projectId: number, pullId: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { pulls } = getState()
  if (
    !pulls.clones ||
    (pulls.clones.length > 0 &&
      (pulls.clones[0].to.projectId !== projectId || pulls.clones[0].to.pullId !== pullId))
  ) {
    try {
      dispatch(fetchingPulls(true))
      const result = await getClones(projectId, pullId)
      dispatch(setClones(result))
    } catch (e) {
      console.log(e)
      dispatch(setPulls([]))
    } finally {
      dispatch(fetchingPulls(false))
    }
  }
}
