import React from 'react'
import { Navbar as BSNavbar } from 'react-bootstrap'

import NavbarLinks from './NavbarLinks'

interface AdminNavbarProps {
  loggedIn: boolean
  brandText: string
}

const Navbar = (props: AdminNavbarProps) => {
  const { brandText, loggedIn } = props
  return (
    <BSNavbar fluid>
      <BSNavbar.Brand>{brandText}</BSNavbar.Brand>
      <BSNavbar.Toggle
        onClick={e => {
          e.preventDefault()
          document.documentElement.classList.toggle('nav-open')
        }}
      />
      <BSNavbar.Collapse>
        <NavbarLinks loggedIn={loggedIn} />
      </BSNavbar.Collapse>
    </BSNavbar>
  )
}

export default Navbar
