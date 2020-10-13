import { save, load, RLSOptions } from 'redux-localstorage-simple'

const options = { states: ['settings'], namespace: 'accula' } as RLSOptions

export const saveState = () => save(options)
export const loadState = () => load(options)
