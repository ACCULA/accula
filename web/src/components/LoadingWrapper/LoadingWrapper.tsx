import { useSnackbar } from 'notistack'
import React, { PropsWithChildren } from 'react'
import { getNotifier } from 'App'
import { Wrapper } from 'store/wrapper'
import { CircularProgress } from '@material-ui/core'
import { useStyles } from './styles'

interface LoadingWrapperProps {
  deps: Wrapper<any>[]
  showError?: boolean
}

const LoadingWrapper: React.FC<LoadingWrapperProps> = ({
  deps,
  showError,
  children
}: PropsWithChildren<LoadingWrapperProps>) => {
  const classes = useStyles()
  const snackbarContext = useSnackbar()
  const isFetching = deps.some(v => v.isFetching || v.value === undefined)
  const errors = deps.map(v => v.error).filter(e => e !== undefined)

  if (showError && errors.length > 0) {
    errors.forEach(err => {
      getNotifier('error', snackbarContext)(err)
    })
    return <>{children}</>
  }

  return isFetching ? (
    <CircularProgress color="secondary" className={classes.progress} />
  ) : (
    <>{children}</>
  )
}

export default LoadingWrapper
