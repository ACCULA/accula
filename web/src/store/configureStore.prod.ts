import { applyMiddleware, createStore } from 'redux'
import thunk from 'redux-thunk'
import { rootReducer } from './reducers'
import { loadState, saveState } from './saveState'

const createStoreWithMiddleware = applyMiddleware(saveState(), thunk)(createStore)

export const configureStoreProd = () => createStoreWithMiddleware(rootReducer, loadState())
