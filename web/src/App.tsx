import React, { useEffect } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { Redirect, Route, Switch, useHistory } from 'react-router-dom'
import { useLocation, useWindowSize } from 'react-use'
import { Helmet } from 'react-helmet'
import { bindActionCreators } from 'redux'

import Navbar from 'components/Navbars'
import Sidebar from 'components/Sidebar'
import Footer from 'components/Footer'
import { routes } from 'routes'
import { AppDispatch, AppState } from 'store'
import { getCurrentUserAction } from 'store/users/actions'

const mapStateToProps = (state: AppState) => ({
  user: state.users.user,
  isFetching: state.users.isFetching
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getCurrentUser: bindActionCreators(getCurrentUserAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type AppProps = ConnectedProps<typeof connector>

const App = ({ isFetching, user, getCurrentUser }: AppProps) => {
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
  }, [getCurrentUser])

  if (isFetching) {
    return <></>
  }

  const availableRoutes = routes.filter(r => user || !r.authRequired)

  return (
    <div className="wrapper">
      <Helmet>
        <title>ACCULA</title>
      </Helmet>
      <Sidebar user={user} routes={availableRoutes} />
      <div id="main-panel" className="main-panel">
        <Navbar user={user} />
        <Switch>
          {availableRoutes.map(route => (
            <Route
              key={route.path}
              path={route.path}
              component={route.component}
              exact={route.exact}
            />
          ))}
          <Redirect to="/projects" path="/" exact />
          <Route>
            <h1 className="text-center">404</h1>
          </Route>
        </Switch>
        <Footer />
      </div>
    </div>
  )
}

export default connector(App)
