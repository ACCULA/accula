import React, { useState } from 'react'
import { IconButton, Tooltip, useTheme } from '@material-ui/core'
import GitHubIcon from '@material-ui/icons/GitHub'
import { Brightness4Rounded, Brightness7Rounded } from '@material-ui/icons'
import { connect, ConnectedProps } from 'react-redux'
import { AppState } from 'store'
import { API_URL } from 'utils'
import { MenuBar } from './MenuBar'
import { useStyles } from './styles'

interface IconsBarProps extends PropsFromRedux {
  setTheme: () => void
}

const IconsBar: React.FC<IconsBarProps> = ({ user, setTheme }: IconsBarProps) => {
  const classes = useStyles()
  const theme = useTheme()
  const [lightTheme, setLightTheme] = useState(theme.palette.type === 'light')

  const toggleTheme = () => {
    setLightTheme(!lightTheme)
    setTheme()
  }

  if (user.isFetching == null || user.isFetching) {
    return <></>
  }

  return (
    <>
      <Tooltip title="Toggle light/dark theme">
        <IconButton color="default" aria-label="Toggle light/dark theme" onClick={toggleTheme}>
          {(lightTheme && <Brightness4Rounded />) || <Brightness7Rounded />}
        </IconButton>
      </Tooltip>
      {user.value ? (
        <MenuBar />
      ) : (
        <Tooltip title="Log in with GitHub">
          <a href={`${API_URL}/api/login/github`} className={classes.login}>
            <IconButton color="default" aria-label="Log in with GitHub">
              <GitHubIcon />
            </IconButton>
          </a>
        </Tooltip>
      )}
    </>
  )
}

const mapStateToProps = (state: AppState) => ({
  user: state.users.user
})

const mapDispatchToProps = () => ({})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(IconsBar)
