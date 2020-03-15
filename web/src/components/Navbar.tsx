import React from 'react'
import { LinkContainer } from 'react-router-bootstrap'
import { Button, Form, Nav, Navbar as BSNavbar } from 'react-bootstrap'

export const Navbar: React.FC = () => {
  return (
    <BSNavbar bg="light" variant="light">
      <BSNavbar.Brand>
        <LinkContainer to="/">
          <Nav.Link>ACCULA</Nav.Link>
        </LinkContainer>
      </BSNavbar.Brand>
      <Nav className="mr-auto">
        <LinkContainer to="/">
          <Nav.Link>Home</Nav.Link>
        </LinkContainer>
        <LinkContainer to="/about">
          <Nav.Link>About</Nav.Link>
        </LinkContainer>
      </Nav>
      <Form inline>
        <Form.Control type="text" placeholder="Search" className="mr-sm-2" />
        <Button variant="outline-primary">Search</Button>
      </Form>
    </BSNavbar>
  )
}
