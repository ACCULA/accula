import React, { useEffect } from 'react'
import { Route, RouteComponentProps, Switch } from 'react-router-dom'

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
    routes.filter(route => route.path === location.pathname)[0]?.name ||
    'ACCULA'

  return (
    <div className="wrapper">
      <Sidebar {...props} routes={routes} color="black" />
      <div id="main-panel" className="main-panel">
        <AdminNavbar {...props} brandText={brand} />
        <Switch>
          {routes.map(route => (
            <Route
              key={route.path}
              path={route.path}
              component={route.component}
              exact={route.exact}
            />
          ))}
        </Switch>
        <Footer />
      </div>
    </div>
  )
}

export default App
