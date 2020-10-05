import React, { useState } from 'react'
import { IconButton, Tooltip, useTheme } from '@material-ui/core'
import GitHubIcon from '@material-ui/icons/GitHub'
import {
  Brightness4Rounded,
  Brightness7Rounded,
  MenuRounded,
  ChevronLeftRounded,
  ChevronRightRounded
} from '@material-ui/icons'
import { connect, ConnectedProps } from 'react-redux'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { changeSettingsAction } from 'store/settings/actions'
import { API_URL } from 'utils'
import { MenuBar } from './MenuBar'
import { useStyles } from './styles'

interface IconsBarProps extends PropsFromRedux {
  setTheme: () => void
}

const IconsBar: React.FC<IconsBarProps> = ({
  user,
  settings,
  setTheme,
  changeSettings
}: IconsBarProps) => {
  const classes = useStyles()
  const theme = useTheme()
  const [lightTheme, setLightTheme] = useState(theme.palette.type === 'light')

  const toggleTheme = () => {
    setLightTheme(!lightTheme)
    setTheme()
  }

  const handleDrawerOpen = () => {
    changeSettings({ ...settings, isDrawerOpen: true })
  }

  const handleDrawerClose = () => {
    changeSettings({ ...settings, isDrawerOpen: false })
  }

  return (
    <>
      {(!settings.isDrawerOpen && (
        <IconButton
          color="default"
          aria-label="open drawer"
          onClick={handleDrawerOpen}
          edge="start"
          className={classes.menuButton}
        >
          <MenuRounded />
        </IconButton>
      )) || (
        <IconButton color="default" aria-label="close drawer" onClick={handleDrawerClose}>
          {theme.direction === 'ltr' ? <ChevronLeftRounded /> : <ChevronRightRounded />}
        </IconButton>
      )}
      {!(user.isFetching == null || user.isFetching) && (
        <div className={classes.mainTools}>
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
        </div>
      )}
    </>
  )
}

const mapStateToProps = (state: AppState) => ({
  user: state.users.user,
  settings: state.settings.settings
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  changeSettings: bindActionCreators(changeSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(IconsBar)
