import React, { useEffect } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Redirect, Route, Switch, useHistory } from 'react-router-dom'
import { useLocation, useWindowSize } from 'react-use'

import Navbar from 'components/Navbars'
import Sidebar from 'components/Sidebar'
import Footer from 'components/Footer'

import { routes } from 'routes'
import { AppDispatch, AppState } from 'store'
import { getAccessTokenAction, getCurrentUserAction, getUserAction } from 'store/users/actions'

const mapStateToProps = (state: AppState) => ({
  token: state.users.token,
  user: state.users.user,
  isFetching: state.users.isFetching
})

const mapDispatchToProps = (dispatch: AppDispatch) =>
  bindActionCreators(
    {
      getAccessToken: getAccessTokenAction,
      getUser: getUserAction,
      getCurrentUser: getCurrentUserAction
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
type AppProps = ConnectedProps<typeof connector>

const App = ({ getAccessToken, getCurrentUser, token, user, isFetching }: AppProps) => {
  const location = useLocation()
  const history = useHistory()
  const { width } = useWindowSize()

  useEffect(() => {
    if (width < 991 && document.documentElement.className.indexOf('nav-open') !== -1) {
      document.documentElement.classList.toggle('nav-open')
    }
  }, [width, location])

  useEffect(() => {
    if (history.action === 'PUSH') {
      document.documentElement.scrollTop = 0
      document.scrollingElement.scrollTop = 0
    }
  }, [history, location])

  useEffect(() => {
    getCurrentUser()
  }, [getCurrentUser, token])

  useEffect(() => {
    getAccessToken()
  }, [getAccessToken])

  if (isFetching) {
    return <></>
  }

  const brand: string =
    routes.filter(route => location.pathname.indexOf(route.path) >= 0)[0]?.name || 'ACCULA'

  return (
    <div className="wrapper">
      <Sidebar user={user} routes={routes} />
      <div id="main-panel" className="main-panel">
        <Navbar user={user} brandText={brand} />
        <Switch>
          {routes.map(route => (
            <Route
              key={route.path}
              path={route.path}
              component={route.component}
              exact={route.exact}
            />
          ))}
          <Redirect to="/projects" path="/" exact />
        </Switch>
        <Footer />
      </div>
    </div>
  )
}

export default connector(App)
