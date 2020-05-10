import React from 'react'
import { Redirect, useLocation } from 'react-router-dom'
import { setAccessToken } from 'accessToken'

const OAuth2RedirectHandler = () => {
  const location = useLocation()
  const accessToken = new URLSearchParams(location.search).get('accessToken')
  const error = new URLSearchParams(location.search).get('error')

  if (accessToken) {
    setAccessToken(accessToken)
    return (
      <Redirect
        to={{
          pathname: '/',
          state: { from: location }
        }}
      />
    )
  }

  return (
    <Redirect
      to={{
        pathname: '/',
        state: {
          from: location,
          error
        }
      }}
    />
  )
}

export default OAuth2RedirectHandler
