import React from 'react'
import { MenuItem, Nav, NavDropdown, NavItem } from 'react-bootstrap'
import { API_URL } from 'utils'

const NavDropdownHack: any = NavDropdown

interface NavbarLinksProps {
  loggedIn: boolean
}

const NavbarLinks = ({ loggedIn }: NavbarLinksProps) => {
  const notification = (
    <div>
      <i className="fa fa-globe" />
      <b className="caret" />
      <span className="notification">5</span>
      <p className="hidden-lg hidden-md">Notification</p>
    </div>
  )
  return (
    <div>
      <Nav>
        <NavDropdownHack eventKey={2} title={notification} noCaret id="basic-nav-dropdown">
          <MenuItem eventKey={2.1}>Notification 1</MenuItem>
          <MenuItem eventKey={2.2}>Notification 2</MenuItem>
          <MenuItem eventKey={2.3}>Notification 3</MenuItem>
          <MenuItem eventKey={2.4}>Notification 4</MenuItem>
          <MenuItem eventKey={2.5}>Another notifications</MenuItem>
        </NavDropdownHack>
        <NavItem eventKey={3} href="#">
          <i className="fa fa-search" />
          <p className="hidden-lg hidden-md">Search</p>
        </NavItem>
      </Nav>

      <Nav pullRight>
        {loggedIn ? (
          <>
            <NavDropdown eventKey={2} title="Dropdown" id="basic-nav-dropdown-right">
              <MenuItem eventKey={2.1}>Action</MenuItem>
              <MenuItem eventKey={2.2}>Another action</MenuItem>
              <MenuItem eventKey={2.3}>Something</MenuItem>
              <MenuItem eventKey={2.4}>Another action</MenuItem>
              <MenuItem eventKey={2.5}>Something</MenuItem>
              <MenuItem divider />
              <MenuItem eventKey={2.5}>Separated link</MenuItem>
            </NavDropdown>
            <NavItem href={`${API_URL}/logout`} className="navbar-link">
              <i className="fab fa-fw fa-sign-out-alt" /> Log out
            </NavItem>
          </>
        ) : (
          <NavItem href={`${API_URL}/login/github`} id="navbar-link">
            <i className="fab fa-fw fa-github" /> Sign in with Github
          </NavItem>
        )}
      </Nav>
    </div>
  )
}

export default NavbarLinks
