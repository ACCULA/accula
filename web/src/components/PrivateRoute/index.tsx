import React from 'react'
import { Redirect, Route, RouteProps } from 'react-router-dom'
import { getNotifier } from 'App'
import { useSnackbar } from 'notistack'

interface PrivateRouteProps extends RouteProps {
  auth: boolean
}

export const PrivateRoute = ({ component: Component, auth, ...rest }: PrivateRouteProps) => {
  const snackbarContext = useSnackbar()
  console.log(auth)
  if (!auth) {
    getNotifier('error', snackbarContext)('Authentication required')
    return (
      <Redirect
        to={{
          pathname: '/'
        }}
      />
    )
  }

  return <Route {...rest} render={props => <Component {...props} />} />
}
