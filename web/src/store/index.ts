import { applyMiddleware, combineReducers, createStore } from 'redux'
import { composeWithDevTools } from 'redux-devtools-extension'
import thunk from 'redux-thunk'
import logger from 'redux-logger'

import { usersReducer } from './users/reducers'
import { projectsReducer } from './projects/reducers'
import { pullsReducer } from './pulls/reducers'

const rootReducer = combineReducers({
  users: usersReducer,
  projects: projectsReducer,
  pulls: pullsReducer
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
export type AppStateSupplier = () => AppState
