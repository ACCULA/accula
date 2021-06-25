import React, { useEffect, useState } from 'react'
import { Link, useHistory } from 'react-router-dom'

import { hasAtLeastAdminRole, IRouteInfo } from 'types'
import { ReactComponent as FinImg } from 'images/fin.svg'
import {
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Tooltip,
  useMediaQuery,
  useTheme
} from '@material-ui/core'
import { SettingsRounded } from '@material-ui/icons'
import { AppDispatch, AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'
import { bindActionCreators } from 'redux'
import { changeSettingsAction } from 'store/settings/actions'
import { historyPush } from 'utils'
import { useStyles } from './styles'

interface SideBarProps extends PropsFromRedux {
  routes: IRouteInfo[]
}

const SideBar = ({ routes, settings, changeSettings, token, user }: SideBarProps) => {
  const theme = useTheme()
  const history = useHistory()
  const classes = useStyles()
  const settingsRoute = routes[routes.findIndex(r => r.path === '/settings')]
  const [previousDrawerState, setPreviousDrawerState] = useState(null)
  const [currentRoute, setRoute] = useState<IRouteInfo>(
    routes.find(route => route.path === history.location.pathname)
  )
  const isMdDown = useMediaQuery(theme.breakpoints.down('md'))
  const isMdUp = useMediaQuery(theme.breakpoints.up('md'))

  const handleDrawerOpen = () => {
    changeSettings({ ...settings, isDrawerOpen: true })
  }

  const handleDrawerClose = () => {
    changeSettings({ ...settings, isDrawerOpen: false })
  }

  const handleItemClick = (route: IRouteInfo) => {
    setRoute(route)
    historyPush(history, route.path)
  }

  useEffect(() => {
    setRoute(routes.find(route => history.location.pathname.startsWith(route.path)))
    // eslint-disable-next-line
  }, [history.location.pathname])

  useEffect(() => {
    if (isMdDown && !isMdUp) {
      setPreviousDrawerState(settings.isDrawerOpen)
      handleDrawerClose()
    }
    if (isMdUp && !isMdDown && previousDrawerState) {
      handleDrawerOpen()
    }
    // eslint-disable-next-line
  }, [isMdDown, isMdUp])

  const drawer = (
    <>
      <div className={classes.drawerHeader}>
        <Link to="/" className={classes.logo}>
          <FinImg className={classes.logoImg} />
          <span className={classes.logoText}>CCULA</span>
        </Link>
      </div>
      <List className={classes.itemList}>
        {routes.map(
          route =>
            route.path !== '/settings' &&
            (!route.authRequired || token) && (
              <ListItem
                classes={{ selected: classes.activeItem, disabled: classes.activeItem }}
                button
                divider
                key={route.name}
                selected={route === currentRoute}
                disabled={route === currentRoute}
                onClick={() => handleItemClick(route)}
              >
                <ListItemText
                  disableTypography
                  primary={route.name}
                  classes={{ root: classes.itemText }}
                />
              </ListItem>
            )
        )}
      </List>
      {token && hasAtLeastAdminRole(user) && (
        <div className={classes.drawerBottom}>
          <Tooltip title="Settings">
            <IconButton
              color="inherit"
              aria-label="Settings"
              onClick={() => historyPush(history, settingsRoute.path)}
            >
              <SettingsRounded />
            </IconButton>
          </Tooltip>
        </div>
      )}
    </>
  )

  return (
    <nav className={classes.drawer} aria-label="sidebar">
      <Drawer
        variant="persistent"
        anchor="left"
        open={settings.isDrawerOpen}
        classes={{
          paper: classes.drawerPaper
        }}
        ModalProps={{
          keepMounted: true
        }}
      >
        {drawer}
      </Drawer>
    </nav>
  )
}

const mapStateToProps = (state: AppState) => ({
  token: state.users.token.accessToken,
  user: state.users.user.value,
  settings: state.settings.settings
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  changeSettings: bindActionCreators(changeSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(SideBar)
