import React, { useEffect } from 'react'
import clsx from 'clsx'
import { connect, ConnectedProps } from 'react-redux'
import { Redirect, Route, Switch, useHistory } from 'react-router-dom'
import { useLocation } from 'react-use'
import { Helmet } from 'react-helmet'
import { bindActionCreators } from 'redux'

import { routes } from 'routes'
import { getCurrentUserAction } from 'store/users/actions'
import { changeSettingsAction } from 'store/settings/actions'
import { PrivateRoute } from 'components/PrivateRoute'
import { useTheme } from 'hooks'
import {
  createMuiTheme,
  createStyles,
  makeStyles,
  Theme,
  ThemeProvider
} from '@material-ui/core/styles'
import { CssBaseline } from '@material-ui/core'
import { NavBar } from 'components/NavBar/NavBar'
import { SnackbarProvider } from 'notistack'
import { AppDispatch, AppState } from 'store'
import SideBar from 'components/SideBar'
import { drawerWidth } from 'utils'

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    root: {
      display: 'flex'
    },
    content: {
      flexGrow: 1,
      height: '100vh',
      position: 'relative',
      padding: '63px 147px',
      [theme.breakpoints.down('sm')]: {
        padding: '63px 73px'
      },
      transition: theme.transitions.create('margin', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen
      }),
      marginLeft: -drawerWidth
    },
    contentShift: {
      transition: theme.transitions.create('margin', {
        easing: theme.transitions.easing.easeOut,
        duration: theme.transitions.duration.enteringScreen
      }),
      marginLeft: 0
    },
    drawerHeader: {
      display: 'flex',
      alignItems: 'center',
      padding: theme.spacing(0, 1),
      ...theme.mixins.toolbar,
      justifyContent: 'flex-end'
    }
  })
)

type AppProps = PropsFromRedux

const App = ({ auth, getCurrentUser, settings, changeSettings }: AppProps) => {
  const location = useLocation()
  const history = useHistory()
  const [theme, toggleTheme] = useTheme(settings.themeMode)
  const customTheme = createMuiTheme(theme)
  const classes = useStyles()

  useEffect(() => {
    if (history.action === 'PUSH') {
      document.documentElement.scrollTop = 0
      document.scrollingElement.scrollTop = 0
    }
  }, [history, location])

  useEffect(() => {
    getCurrentUser()
  }, [getCurrentUser])

  const changeTheme = () => {
    changeSettings({ ...settings, themeMode: theme.palette!.type! === 'dark' ? 'light' : 'dark' })
    toggleTheme()
  }

  const sideBarRoutes = routes.filter(r => r.sidebar)

  return (
    <div className={classes.root}>
      <ThemeProvider theme={customTheme}>
        <SnackbarProvider
          maxSnack={3}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right'
          }}
        >
          <CssBaseline />
          <Helmet>
            <title>ACCULA</title>
          </Helmet>
          <NavBar setTheme={changeTheme} />
          <SideBar routes={sideBarRoutes} />
          <main
            className={clsx(classes.content, {
              [classes.contentShift]: settings.isDrawerOpen
            })}
          >
            <div className={classes.drawerHeader} />
            <Switch>
              {routes.map(route =>
                route.authRequired ? (
                  <PrivateRoute
                    key={route.path}
                    path={route.path}
                    component={route.component}
                    exact={route.exact}
                    auth={auth}
                  />
                ) : (
                  <Route
                    key={route.path}
                    path={route.path}
                    component={route.component}
                    exact={route.exact}
                  />
                )
              )}
              <Redirect to="/projects" path="/" exact />
              <Route>
                <h1 className="text-center">404</h1>
              </Route>
            </Switch>
          </main>
        </SnackbarProvider>
      </ThemeProvider>
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  auth: state.users.user.value !== null,
  settings: state.settings.settings
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getCurrentUser: bindActionCreators(getCurrentUserAction, dispatch),
  changeSettings: bindActionCreators(changeSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(App)
