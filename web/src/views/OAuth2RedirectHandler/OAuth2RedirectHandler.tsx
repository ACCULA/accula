import React, { useEffect } from 'react'
import { Redirect, useLocation } from 'react-router-dom'
import { connect, ConnectedProps } from 'react-redux'

import { setAccessTokenAction } from 'store/users/actions'
import { IToken } from 'types'

const mapDispatchToProps = dispatch => ({
  setAccessToken: (token: IToken) => dispatch(setAccessTokenAction(token))
})

const connector = connect(null, mapDispatchToProps)
type OAuth2RedirectHandlerProps = ConnectedProps<typeof connector>

const OAuth2RedirectHandler = ({ setAccessToken }: OAuth2RedirectHandlerProps) => {
  const location = useLocation()
  const accessToken: string = new URLSearchParams(location.search).get('accessToken')
  const error = new URLSearchParams(location.search).get('error')

  useEffect(() => {
    if (accessToken) {
      setAccessToken({ accessToken })
    }
    // eslint-disable-next-line
  }, [])

  if (accessToken) {
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

export default connector(OAuth2RedirectHandler)
