import React from 'react'
import { NavLink } from 'react-router-dom'

export const Navbar: React.FC = () => (
  <nav>
    <NavLink to="/" className="brand-logo">
      ACCULA
    </NavLink>
    <ul className="right hide-on-med-and-down">
      <li>
        <NavLink to="/">Home</NavLink>
      </li>
      <li>
        <NavLink to="/about">About</NavLink>
      </li>
    </ul>
  </nav>
)
