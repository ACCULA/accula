import { CHANGE_SETTINGS, SettingsState, SettingsActionTypes } from './types'

const initialState: SettingsState = {
  settings: {
    themeMode: 'light'
  }
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
    default:
      return state
  }
}
