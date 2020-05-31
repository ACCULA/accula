import { AppDispatch, AppStateSupplier } from 'store'
import { requireToken } from 'store/users/actions'
import { failed, fetched, fetching } from 'store/wrapper'
import {
  SET_CLONES, //
  SET_DIFFS,
  SET_PULL,
  SET_PULLS,
  SetClones,
  SetDiffs,
  SetPull,
  SetPulls
} from './types'
import { getClones, getDiff, getPull, getPulls } from './services'

const setPulls = (payload): SetPulls => ({
  type: SET_PULLS,
  payload
})

const setPull = (payload): SetPull => ({
  type: SET_PULL,
  payload
})

const setClones = (payload): SetClones => ({
  type: SET_CLONES,
  payload
})

const setDiff = (payload): SetDiffs => ({
  type: SET_DIFFS,
  payload
})

export const getPullsAction = (projectId: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { pulls, users } = getState()
  if (pulls.pulls.isFetching) {
    return
  }
  if (
    !pulls.pulls.value ||
    pulls.pulls.value.length === 0 ||
    pulls.pulls.value[0].projectId !== projectId
  ) {
    try {
      dispatch(setPulls(fetching))
      const result = await getPulls(users.token, projectId)
      dispatch(setPulls(fetched(result)))
    } catch (e) {
      dispatch(setPulls(failed(e)))
    }
  }
}

export const getPullAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { pulls, users } = getState()
  if (pulls.pulls.value) {
    const pull = pulls.pulls.value.find(p => p.number === pullNumber && p.projectId === projectId)
    if (pull) {
      dispatch(setPull(fetched(pull)))
      return
    }
  }
  if (pulls.pull.isFetching) {
    return
  }
  if (
    !pulls.pull.value ||
    pulls.pull.value.projectId !== projectId ||
    pulls.pull.value.number !== pullNumber
  ) {
    try {
      dispatch(setPull(fetching))
      const pull = await getPull(users.token, projectId, pullNumber)
      dispatch(setPull(fetched(pull)))
    } catch (e) {
      dispatch(setPull(failed(e)))
    }
  }
}

export const getClonesAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, pulls } = getState()
  if (pulls.clones.isFetching) {
    return
  }
  if (
    !pulls.clones.value || //
    pulls.clones.projectId !== projectId ||
    pulls.clones.pullNumber !== pullNumber
  ) {
    try {
      dispatch(setClones(fetching))
      const result = await getClones(users.token, projectId, pullNumber)
      dispatch(setClones(fetched(result, { projectId, pullNumber })))
    } catch (e) {
      dispatch(setClones(failed(e)))
    }
  }
}

export const getDiffAction = (projectId: number, pullNumber: number) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  await requireToken(dispatch, getState)
  const { users, pulls } = getState()
  if (pulls.diff.isFetching) {
    return
  }
  if (
    !pulls.diff.value || //
    pulls.diff.projectId !== projectId ||
    pulls.diff.pullNumber !== pullNumber
  ) {
    try {
      dispatch(setDiff(fetching))
      const result = await getDiff(users.token, projectId, pullNumber)
      dispatch(setDiff(fetched(result, { projectId, pullNumber })))
    } catch (e) {
      dispatch(setDiff(failed(e)))
    }
  }
}
