import React from 'react'
import { Navbar as BSNavbar } from 'react-bootstrap'

import { User } from 'types'
import logo from 'images/fin_matisse.svg'
import NavbarLinks from './NavbarLinks'

interface AdminNavbarProps {
  brandText: string
  user?: User
}

const Navbar = ({ brandText, user }: AdminNavbarProps) => (
  <BSNavbar fluid>
    <img src={logo} alt="Accula" className="navbar-logo" />
    <BSNavbar.Brand>{brandText}</BSNavbar.Brand>
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

export default Navbar