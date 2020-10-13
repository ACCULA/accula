import { ISettings } from 'types'

export const CHANGE_SETTINGS = 'CHANGE_SETTINGS'

export interface SettingsState {
  settings: ISettings
}

export interface ChangeSettings {
  type: typeof CHANGE_SETTINGS
  settings: ISettings
}

export type SettingsActionTypes = ChangeSettings
