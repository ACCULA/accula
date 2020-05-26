import React from 'react'
import { MenuItem, Nav, NavDropdown, NavItem } from 'react-bootstrap'
import { useHistory } from 'react-router-dom'

import { API_URL } from 'utils'
import { IUser } from 'types'

const NavDropdownHack: any = NavDropdown

interface NavbarLinksProps {
  user?: IUser
}

const NavbarLinks = ({ user }: NavbarLinksProps) => {
  const history = useHistory()
  const notification = (
    <div>
      <i className="fa fa-bell" />
      <b className="caret" />
      <span className="notification">5</span>
      <p className="hidden-lg hidden-md">Notification</p>
    </div>
  )
  return (
    <div>
      {/*<Nav>*/}
      {/*  <NavItem eventKey={3} href="#">*/}
      {/*    <i className="fa fa-search" />*/}
      {/*    <p className="hidden-lg hidden-md">Search</p>*/}
      {/*  </NavItem>*/}
      {/*</Nav>*/}

      <Nav pullRight>
        {user ? (
          <>
            <NavDropdownHack eventKey={2} title={notification} noCaret id="basic-nav-dropdown">
              <MenuItem eventKey={2.1}>Notification 1</MenuItem>
              <MenuItem eventKey={2.2}>Notification 2</MenuItem>
              <MenuItem eventKey={2.3}>Notification 3</MenuItem>
              <MenuItem eventKey={2.4}>Notification 4</MenuItem>
              <MenuItem eventKey={2.5}>Another notifications</MenuItem>
            </NavDropdownHack>
            <NavDropdown
              id="basic-nav-dropdown-right"
              className="navbar-links"
              title={`@${user.login}`}
            >
              <MenuItem onClick={() => history.push('/profile')}>Profile</MenuItem>
              <MenuItem onClick={() => history.push('/settings')}>Settings</MenuItem>
              {/*<MenuItem onClick={() => {}}>Log out</MenuItem>*/}
            </NavDropdown>
          </>
        ) : (
          <NavItem href={`${API_URL}/api/login/github`} id="navbar-link">
            <i className="fab fa-fw fa-github" /> Sign in with Github
          </NavItem>
        )}
      </Nav>
    </div>
  )
}

export default NavbarLinks
