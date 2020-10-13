import { ISettings } from 'types'
import { ChangeSettings, CHANGE_SETTINGS } from './types'

export const changeSettingsAction = (settings: ISettings): ChangeSettings => ({
  type: CHANGE_SETTINGS,
  settings
})
