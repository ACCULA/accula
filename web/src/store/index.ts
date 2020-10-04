import { configureStoreDev } from './configureStore.dev'
import { configureStoreProd } from './configureStore.prod'
import { rootReducer } from './reducers'

const configureStore =
  process.env.NODE_ENV === 'production' ? configureStoreProd : configureStoreDev

export const store = configureStore()
export type AppDispatch = typeof store.dispatch
export type AppState = ReturnType<typeof rootReducer>
export type AppStateSupplier = () => AppState
