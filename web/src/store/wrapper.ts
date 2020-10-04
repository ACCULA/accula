export interface Wrapper<T> {
  isFetching: boolean | null
  value?: T
  error?: string
}

export const notFetching = {
  isFetching: false
}

export const fetching = {
  isFetching: true
}

export const fetched = <T>(value: T, args?: any): Wrapper<T> => ({
  isFetching: false,
  value,
  ...args
})

export const failed = <T>(error: Error): Wrapper<T> => {
  console.log(error)
  return {
    isFetching: false,
    error: error.message,
    value: null
  }
}
