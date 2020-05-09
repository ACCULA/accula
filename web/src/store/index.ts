import { applyMiddleware, combineReducers, createStore } from 'redux'
import { composeWithDevTools } from 'redux-devtools-extension'
import thunk from 'redux-thunk'
import logger from 'redux-logger'

import { usersReducer } from './users/reducers'
import { projectsReducer } from './projects/reducers'

const rootReducer = combineReducers({
  users: usersReducer,
  projects: projectsReducer
})

export const store = createStore(
  rootReducer,
  composeWithDevTools(
    applyMiddleware(
      thunk, //
      logger
    )
  )
)

export type AppDispatch = typeof store.dispatch
export type AppState = ReturnType<typeof rootReducer>
