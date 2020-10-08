import { AppDispatch, AppStateSupplier } from 'store'
import { requireToken } from 'store/users/actions'
import { failed, fetched, fetching, notFetching, Wrapper } from 'store/wrapper'
import { IPull, IShortPull } from 'types'
import {
  IPullClonesState,
  IPullComparesState,
  IPullDiffsState,
  SET_CLONES,
  SET_COMPARES,
  SET_DIFFS,
  SET_PULL,
  SET_PULLS,
  RESET_PULLS_INFO,
  SetClones,
  SetCompares,
  SetDiffs,
  SetPull,
  SetPulls,
  ResetPullsInfo
} from './types'
import { getClones, getCompares, getDiffs, getPull, getPulls, refreshClones } from './services'

const setPulls = (payload: Wrapper<IShortPull[]>): SetPulls => ({
  type: SET_PULLS,
  payload
})

const setPull = (payload: Wrapper<IPull>): SetPull => ({
  type: SET_PULL,
  payload
})

const setCompares = (payload: IPullComparesState): SetCompares => ({
  type: SET_COMPARES,
  payload
})

const setDiffs = (payload: IPullDiffsState): SetDiffs => ({
  type: SET_DIFFS,
  payload
})

const setClones = (payload: IPullClonesState): SetClones => ({
  type: SET_CLONES,
  payload
})

export const resetPullsInfo = (): ResetPullsInfo => ({
  type: RESET_PULLS_INFO
})

export const getPullsAction = (projectId: number, handleError?: (msg: string) => void) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { pulls, users } = getState()
  if (
    pulls.pulls.isFetching ||
    (pulls.pulls.value &&
      pulls.pulls.value.length !== 0 &&
      pulls.pulls.value[0].projectId === projectId)
  ) {
    return
  }
  try {
    dispatch(setPulls(fetching))
    const result = await getPulls(users.token, projectId)
    dispatch(setPulls(fetched(result)))
  } catch (e) {
    dispatch(setPulls(failed(e)))
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const getPullAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { pulls, users } = getState()
  if (
    pulls.pull.isFetching ||
    (pulls.pull.value &&
      pulls.pull.value.projectId === projectId &&
      pulls.pull.value.number === pullNumber)
  ) {
    return
  }
  try {
    dispatch(setPull(fetching))
    const pull = await getPull(users.token, projectId, pullNumber)
    dispatch(setPull(fetched(pull)))
  } catch (e) {
    dispatch(setPull(failed(e)))
  }
}

export const getDiffsAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, pulls } = getState()
  if (
    pulls.diffs.isFetching ||
    (pulls.diffs.value &&
      pulls.diffs.projectId === projectId &&
      pulls.diffs.pullNumber === pullNumber)
  ) {
    return
  }
  try {
    dispatch(setDiffs(fetching))
    const result = await getDiffs(users.token, projectId, pullNumber)
    dispatch(setDiffs(fetched(result, { projectId, pullNumber })))
  } catch (e) {
    dispatch(setDiffs(failed(e)))
  }
}

export const getComparesAction = (projectId: number, target: number, source: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  if (source === 0) {
    dispatch(setCompares(notFetching))
    return
  }
  await requireToken(dispatch, getState)
  const { users, pulls } = getState()
  if (
    pulls.compares.isFetching ||
    (pulls.compares.value &&
      pulls.compares.projectId === projectId &&
      pulls.compares.target === target &&
      pulls.compares.source === source)
  ) {
    return
  }
  try {
    dispatch(setCompares(fetching))
    const result = await getCompares(users.token, projectId, target, source)
    dispatch(setCompares(fetched(result, { projectId, target, source })))
  } catch (e) {
    dispatch(setCompares(failed(e)))
  }
}

export const getClonesAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, pulls } = getState()
  if (
    pulls.clones.isFetching ||
    (pulls.clones.value &&
      pulls.clones.projectId === projectId &&
      pulls.clones.pullNumber === pullNumber)
  ) {
    return
  }
  try {
    dispatch(setClones(fetching))
    const result = await getClones(users.token, projectId, pullNumber)
    dispatch(setClones(fetched(result, { projectId, pullNumber })))
  } catch (e) {
    dispatch(setClones(failed(e)))
  }
}

export const refreshClonesAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users } = getState()
  try {
    dispatch(setClones(fetching))
    const clones = await refreshClones(users.token, projectId, pullNumber)
    dispatch(setClones(fetched(clones, { projectId, pullNumber })))
  } catch (e) {
    dispatch(setClones(failed(e)))
  }
}
