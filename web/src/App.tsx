import React, { useEffect } from 'react'
import { bindActionCreators } from 'redux'
import { connect, ConnectedProps } from 'react-redux'
import { Redirect, Route, RouteComponentProps, Switch } from 'react-router-dom'
import jwtDecode from 'jwt-decode'

import Navbar from 'components/Navbars'
import Sidebar from 'components/Sidebar'
import Footer from 'components/Footer'

import { getAccessToken } from 'accessToken'
import { routes } from 'routes'
import { AppDispatch, AppState } from 'store'
import { getUserAction } from 'store/users/actions'

const mapStateToProps = (state: AppState) => ({
  users: state.users
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getUser: bindActionCreators(getUserAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)
type PropsFromRedux = ConnectedProps<typeof connector>
type AppProps = PropsFromRedux & RouteComponentProps

const App = (props: AppProps) => {
  const { history, location, getUser, users } = props

  useEffect(() => {
    if (history.action === 'PUSH') {
      document.documentElement.scrollTop = 0
      document.scrollingElement.scrollTop = 0
      document.documentElement.classList.toggle('nav-open')
    }
  })

  useEffect(() => {
    getAccessToken().then(accessToken => {
      if (accessToken && accessToken !== '') {
        const { sub } = jwtDecode(accessToken)
        getUser(parseInt(sub, 10))
      }
    })
  }, [getUser])

  if (users.isFetching) {
    return <></>
  }

  const brand: string =
    routes.filter(route => location.pathname.indexOf(route.path) >= 0)[0]?.name || 'ACCULA'

  return (
    <div className="wrapper">
      <Sidebar {...props} user={users.user} routes={routes} color="black" />
      <div id="main-panel" className="main-panel">
        <Navbar {...props} user={users.user} brandText={brand} />
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
