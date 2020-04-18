import React from 'react'
import { Link, NavLink, RouteComponentProps } from 'react-router-dom'

import { RouteInfo } from 'types'
import logo from 'images/logo.png'
import { useWindowSize } from 'react-use'
import NavbarLinks from '../Navbars/NavbarLinks'

interface SidebarProps extends RouteComponentProps {
  routes: RouteInfo[]
  color: string
}

const Sidebar = (props: SidebarProps) => {
  const { routes, color, location } = props
  const { width } = useWindowSize()

  return (
    <div id="sidebar" className="sidebar" data-color={color}>
      <div className="logo">
        <Link className="simple-text logo-mini" to="/">
          <div className="logo-img">
            <img src={logo} alt="logo_image" />
          </div>
        </Link>
        <Link to="/" className="simple-text logo-normal">
          ACCULA
        </Link>
      </div>
      <div className="sidebar-wrapper">
        <ul className="nav">
          {width <= 991 ? <NavbarLinks /> : null}
          {routes.map(
            route =>
              !route.hide && (
                <li className={location.pathname === route.path ? 'active' : ''} key={route.path}>
                  <NavLink to={route.path} className="nav-link" activeClassName="active">
                    <i className={route.icon} />
                    <p>{route.name}</p>
                  </NavLink>
                </li>
              )
          )}
        </ul>
      </div>
    </div>
  )
}

export default Sidebar
