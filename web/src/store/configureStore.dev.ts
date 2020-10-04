import { applyMiddleware, createStore } from 'redux'
import { composeWithDevTools } from 'redux-devtools-extension'
import thunk from 'redux-thunk'
import logger from 'redux-logger'
import { rootReducer } from './reducers'
import { saveState, loadState } from './saveState'

const createStoreWithMiddleware = composeWithDevTools(applyMiddleware(saveState(), thunk, logger))(
  createStore
)

export const configureStoreDev = () => createStoreWithMiddleware(rootReducer, loadState())
