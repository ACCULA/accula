import React from 'react'
import { AppBar, Toolbar, useTheme } from '@material-ui/core'
import { IconsBar } from './IconsBar'
import { useStyles } from './styles'

interface NavBarProps {
  setTheme: () => void
}

const NavBar: React.FC<NavBarProps> = ({ setTheme }: NavBarProps) => {
  const theme = useTheme()
  const classes = useStyles(theme.palette.type)
  return (
    <AppBar className={classes.appBar}>
      <Toolbar className={classes.toolBar}>
        <IconsBar setTheme={setTheme} />
      </Toolbar>
    </AppBar>
  )
}

export default NavBar
