import React, { useEffect, useState } from 'react'
import { Redirect, Route, RouteComponentProps, Switch } from 'react-router-dom'

import AdminNavbar from 'components/Navbars/Navbar'
import Footer from 'components/Footer/Footer'
import Sidebar from 'components/Sidebar/Sidebar'

import routes from 'routes'
import { getAccessToken } from 'accessToken'
import OAuth2RedirectHandler from 'OAuth2RedirectHandler'

const App = (props: RouteComponentProps) => {
  const { history, location } = props

  useEffect(() => {
    if (
      window.innerWidth < 993 &&
      history.location.pathname !== location.pathname &&
      document.documentElement.className.indexOf('nav-open') !== -1
    ) {
      document.documentElement.classList.toggle('nav-open')
    }
    if (history.action === 'PUSH') {
      document.documentElement.scrollTop = 0
      document.scrollingElement.scrollTop = 0
    }
  })

  const brand: string =
    routes.filter(route => location.pathname.indexOf(route.path) >= 0)[0]?.name || 'ACCULA'

  const [loading, setLoading] = useState(true)
  const [loggedIn, setLoggedIn] = useState(false)

  useEffect(() => {
    getAccessToken()
      .then(accessToken => {
        if (accessToken && accessToken !== '') {
          setLoggedIn(true)
        } else {
          setLoggedIn(false)
        }
        setLoading(false)
      })
      .catch(() => {
        setLoading(false)
      })
  }, [])

  return (
    <div className="wrapper">
      <Sidebar {...props} routes={routes} color="black" loggedIn={loggedIn} />
      <div id="main-panel" className="main-panel">
        <AdminNavbar {...props} brandText={brand} loggedIn={loggedIn} />
        {loading && <h2>Loading...</h2>}
        <Switch>
          <Route
            path="/oauth2/redirect"
            component={OAuth2RedirectHandler}
            exact
          />
          {routes.map(route => (
            <Route
              key={route.path}
              path={route.path}
              component={route.component}
              exact={route.exact}
            />
          ))}
          <Redirect to="/projects" path="/" exact/>
        </Switch>
        <Footer />
      </div>
    </div>
  )
}

export default App
