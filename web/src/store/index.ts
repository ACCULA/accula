import { applyMiddleware, combineReducers, createStore } from 'redux'
import { composeWithDevTools } from 'redux-devtools-extension'
import thunk from 'redux-thunk'

import { usersReducer } from './users/reducers'

const rootReducer = combineReducers({
  users: usersReducer
})

export const store = createStore(
  rootReducer,
  composeWithDevTools(applyMiddleware(
    thunk
  ))
)

export type AppDispatch = typeof store.dispatch
export type AppState = ReturnType<typeof rootReducer>
