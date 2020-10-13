import React from 'react'
import { Toolbar } from '@material-ui/core'
import { NavBar } from './NavBar'

interface NavBarContainerProps {
  setTheme: () => void
}

const NavBarContainer: React.FC<NavBarContainerProps> = ({ setTheme }: NavBarContainerProps) => {
  return (
    <div>
      <NavBar setTheme={setTheme} />
      <Toolbar id="back-to-top-anchor" />
    </div>
  )
}

export default NavBarContainer
