import { CHANGE_SETTINGS, SettingsState, SettingsActionTypes, UPDATE_APP_SETTINGS } from './types'
import { notFetching } from '../wrapper'

const initialState: SettingsState = {
  settings: {
    themeMode: 'light',
    isDrawerOpen: true,
    splitCodeView: 'unified'
  },
  appSettings: notFetching
}

export function settingsReducer(
  state: SettingsState = initialState, //
  action: SettingsActionTypes
): SettingsState {
  switch (action.type) {
    case CHANGE_SETTINGS: {
      return {
        ...state,
        settings: action.settings
      }
    }
    case UPDATE_APP_SETTINGS: {
      return {
        ...state,
        appSettings: action.payload
      }
    }
    default:
      return state
  }
}
