import React from 'react'
import { Navbar as BSNavbar } from 'react-bootstrap'
import { useLocation } from 'react-use'

import logo from 'images/fin_matisse.svg'
import { routes } from 'routes'
import { IUser } from 'types'
import NavbarLinks from './NavbarLinks'

interface NavbarProps {
  user?: IUser
}

const Navbar = ({ user }: NavbarProps) => {
  const location = useLocation()
  const brand: string =
    routes.filter(route => location.pathname.indexOf(route.path) >= 0)[0]?.name || 'ACCULA'

  return (
    <BSNavbar fluid>
      <img src={logo} alt="Accula" className="navbar-logo" />
      <BSNavbar.Brand>{brand}</BSNavbar.Brand>
      <BSNavbar.Toggle
        onClick={(e: React.MouseEvent<any>) => {
          e.preventDefault()
          document.documentElement.classList.toggle('nav-open')
        }}
      />
      <BSNavbar.Collapse>
        <NavbarLinks user={user} />
      </BSNavbar.Collapse>
    </BSNavbar>
  )
}

export default Navbar
