import { applyMiddleware, createStore } from 'redux'
import { rootReducer } from './reducers'
import { loadState, saveState } from './saveState'

const createStoreWithMiddleware = applyMiddleware(saveState())(createStore)

export const configureStoreProd = () => createStoreWithMiddleware(rootReducer, loadState())
