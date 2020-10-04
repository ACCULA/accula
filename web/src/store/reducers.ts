import { combineReducers } from 'redux'
import { projectsReducer } from './projects/reducers'
import { pullsReducer } from './pulls/reducers'
import { settingsReducer } from './settings/reducers'
import { usersReducer } from './users/reducers'

export const rootReducer = combineReducers({
  users: usersReducer,
  projects: projectsReducer,
  pulls: pullsReducer,
  settings: settingsReducer
})
