import React from 'react'
import { Route, RouteProps } from 'react-router-dom'
import { AuthWall } from 'components/AuthWall'

interface PrivateRouteProps extends RouteProps {
  auth: boolean
}

export const PrivateRoute = ({ component: Component, auth, ...rest }: PrivateRouteProps) => {
  return (
    <Route
      {...rest}
      render={props =>
        auth ? ( //
          <Component {...props} />
        ) : (
          <AuthWall />
        )
      }
    />
  )
}
