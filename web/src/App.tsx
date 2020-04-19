import React, { useEffect } from 'react'
import { Redirect, Route, RouteComponentProps, Switch } from 'react-router-dom'

import AdminNavbar from 'components/Navbars/Navbar'
import Footer from 'components/Footer/Footer'
import Sidebar from 'components/Sidebar/Sidebar'

import routes from 'routes'

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

  const loggedIn = false

  return (
    <div className="wrapper">
      <Sidebar {...props} routes={routes} color="black" loggedIn={loggedIn} />
      <div id="main-panel" className="main-panel">
        <AdminNavbar {...props} brandText={brand} loggedIn={loggedIn} />
        <Switch>
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
