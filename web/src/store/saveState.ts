import { save, load, RLSOptions } from 'redux-localstorage-simple'

const options: RLSOptions = { states: ['settings'], namespace: 'accula' }

export const saveState = () => save(options)
export const loadState = () => load(options)
