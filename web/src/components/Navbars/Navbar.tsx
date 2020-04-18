import React from 'react'
import { Navbar as BSNavbar } from 'react-bootstrap'

import NavbarLinks from 'components/Navbars/NavbarLinks'

interface AdminNavbarProps {
  brandText: string
}

const Navbar = (props: AdminNavbarProps) => {
  const { brandText } = props
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
        <NavbarLinks />
      </BSNavbar.Collapse>
    </BSNavbar>
  )
}

export default Navbar
