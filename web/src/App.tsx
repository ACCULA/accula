import React, { useEffect } from 'react'
import clsx from 'clsx'
import { connect, ConnectedProps } from 'react-redux'
import { Redirect, Route, Switch } from 'react-router-dom'
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
import { DRAWER_WIDTH } from 'utils'
import { PageTitle } from 'components/PageTitle'
import { NotFound } from 'views/NotFound'

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    content: {
      flexGrow: 1,
      height: '100vh',
      position: 'relative',
      padding: '63px 147px',
      [theme.breakpoints.down('sm')]: {
        marginLeft: 0,
        padding: '63px 73px'
      },
      transition: theme.transitions.create('margin', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen
      })
    },
    contentShift: {
      transition: theme.transitions.create('margin', {
        easing: theme.transitions.easing.easeOut,
        duration: theme.transitions.duration.enteringScreen
      }),
      marginLeft: DRAWER_WIDTH
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

const App = ({ auth, token, getCurrentUser, settings, changeSettings }: AppProps) => {
  const [theme, toggleTheme] = useTheme(settings.themeMode)
  const customTheme = createMuiTheme(theme)
  const classes = useStyles()

  useEffect(() => {
    getCurrentUser()
    // eslint-disable-next-line
  }, [])

  if (token === null) {
    return <></>
  }

  const changeTheme = () => {
    changeSettings({ ...settings, themeMode: theme.palette!.type! === 'dark' ? 'light' : 'dark' })
    toggleTheme()
  }

  const sideBarRoutes = routes.filter(r => r.sidebar)

  return (
    <div>
      <ThemeProvider theme={customTheme}>
        <SnackbarProvider
          maxSnack={3}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right'
          }}
        >
          <CssBaseline />
          <PageTitle />
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
              <Route component={NotFound} />
            </Switch>
          </main>
        </SnackbarProvider>
      </ThemeProvider>
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  auth: state.users.user.value !== null,
  token: state.users.token.accessToken,
  settings: state.settings.settings
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  getCurrentUser: bindActionCreators(getCurrentUserAction, dispatch),
  changeSettings: bindActionCreators(changeSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(App)
