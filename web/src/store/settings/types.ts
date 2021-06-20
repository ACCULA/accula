import { IAppSettings, ISettings } from 'types'
import { Wrapper } from '../wrapper'

export const CHANGE_SETTINGS = 'CHANGE_SETTINGS'
export const UPDATE_APP_SETTINGS = 'UPDATE_APP_SETTINGS'

export interface SettingsState {
  settings: ISettings
  appSettings: Wrapper<IAppSettings>
}

export interface ChangeSettings {
  type: typeof CHANGE_SETTINGS
  settings: ISettings
}

export interface UpdateAppSettings {
  type: typeof UPDATE_APP_SETTINGS
  payload: Wrapper<IAppSettings>
}

export type SettingsActionTypes =
  | ChangeSettings //
  | UpdateAppSettings
