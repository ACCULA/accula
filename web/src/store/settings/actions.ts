import { IAppSettings, ISettings } from 'types'
import { CHANGE_SETTINGS, ChangeSettings, UPDATE_APP_SETTINGS, UpdateAppSettings } from './types'
import { AppDispatch, AppStateSupplier } from '../index'
import { requireToken } from '../users/actions'
import { failed, fetched, fetching, Wrapper } from '../wrapper'
import { getAppSettings, putAppSettings } from './services'

export const changeSettingsAction = (settings: ISettings): ChangeSettings => ({
  type: CHANGE_SETTINGS,
  settings
})

export const updateAppSettings = (payload: Wrapper<IAppSettings>): UpdateAppSettings => ({
  type: UPDATE_APP_SETTINGS,
  payload
})

export const getAppSettingsAction = (handleError?: (msg: string) => void) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { settings } = getState()
  if (settings.appSettings.isFetching) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  if (!users.token) {
    return
  }
  try {
    dispatch(updateAppSettings(fetching))
    const appSettings = await getAppSettings(users.token)
    dispatch(updateAppSettings(fetched(appSettings)))
  } catch (e) {
    dispatch(updateAppSettings(failed(e)))
    if (handleError) {
      handleError(e.message)
    }
  }
}

export const updateAppSettingsAction = (
  appSettings: IAppSettings,
  handleSuccess?: () => void,
  handleError?: (msg: string) => void
) => async (
  dispatch: AppDispatch, //
  getState: AppStateSupplier
) => {
  const { settings } = getState()
  if (settings.appSettings.isFetching) {
    return
  }
  await requireToken(dispatch, getState)
  const { users } = getState()
  if (!users.token) {
    return
  }
  try {
    dispatch(updateAppSettings(fetching))
    const updatedAppSettings = await putAppSettings(users.token, appSettings)
    dispatch(updateAppSettings(fetched(updatedAppSettings)))
    if (handleSuccess) {
      handleSuccess()
    }
  } catch (e) {
    dispatch(updateAppSettings(failed(e)))
    if (handleError) {
      handleError(e.message)
    }
  }
}
