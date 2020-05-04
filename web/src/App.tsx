import React, { useEffect, useState } from 'react'
import { Redirect, Route, RouteComponentProps, Switch } from 'react-router-dom'

import Navbar from 'components/Navbars'
import Sidebar from 'components/Sidebar'
import Footer from 'components/Footer'

import routes from 'routes'
import { getAccessToken } from 'accessToken'

const App = (props: RouteComponentProps) => {
  const { history, location } = props

  useEffect(() => {
    if (history.action === 'PUSH') {
      document.documentElement.scrollTop = 0
      document.scrollingElement.scrollTop = 0
      document.documentElement.classList.toggle('nav-open')
    }
  })

  const [loading, setLoading] = useState(true)
  const [loggedIn, setLoggedIn] = useState(false)

  useEffect(() => {
    getAccessToken()
      .then(accessToken => {
        setLoggedIn(accessToken && accessToken !== '')
      })
      .finally(() => {
        setLoading(false)
      })
  })

  if (loading) {
    return <></>
  }

  const brand: string =
    routes.filter(route => location.pathname.indexOf(route.path) >= 0)[0]?.name || 'ACCULA'

  return (
    <div className="wrapper">
      <Sidebar {...props} routes={routes} color="black" loggedIn={loggedIn} />
      <div id="main-panel" className="main-panel">
        <Navbar {...props} brandText={brand} loggedIn={loggedIn} />
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

export default App
