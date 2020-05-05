import React from 'react'
import { Link, NavLink, RouteComponentProps } from 'react-router-dom'
import { useWindowSize } from 'react-use'

import { RouteInfo, User } from 'types'
import logo from 'images/fin_tango.svg'
import NavbarLinks from 'components/Navbars/NavbarLinks'

interface SidebarProps extends RouteComponentProps {
  routes: RouteInfo[]
  color: string
  user?: User
}

const Sidebar = ({ routes, color, location, user }: SidebarProps) => {
  const { width } = useWindowSize()

  return (
    <div id="sidebar" className="sidebar" data-color={color}>
      <Link to="/" className="logo">
        <img src={logo} alt="A" />
        <span>CCULA</span>
      </Link>
      <div className="sidebar-wrapper">
        <ul className="nav">
          {routes
            .filter(route => !route.hidden)
            .map(route => (
              <li className={location.pathname === route.path ? 'active' : ''} key={route.path}>
                <NavLink to={route.path} className="nav-link" activeClassName="active">
                  <i className={`fas fa-${route.icon}`} />
                  <p>{route.name}</p>
                </NavLink>
              </li>
            ))}
          {width <= 991 ? <NavbarLinks user={user} /> : null}
        </ul>
      </div>
    </div>
  )
}

export default Sidebar
