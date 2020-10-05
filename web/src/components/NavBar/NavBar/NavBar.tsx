import React from 'react'
import clsx from 'clsx'
import { connect, ConnectedProps } from 'react-redux'
import { AppDispatch, AppState } from 'store'
import { bindActionCreators } from 'redux'
import { changeSettingsAction } from 'store/settings/actions'
import { AppBar, Toolbar, useTheme } from '@material-ui/core'
import { IconsBar } from './IconsBar'
import { useStyles } from './styles'

interface NavBarProps extends PropsFromRedux {
  setTheme: () => void
}

const NavBar: React.FC<NavBarProps> = ({ settings, setTheme }: NavBarProps) => {
  const theme = useTheme()
  const classes = useStyles(theme.palette.type)
  return (
    <AppBar
      position="fixed"
      className={clsx(classes.appBar, {
        [classes.appBarShift]: settings.isDrawerOpen
      })}
    >
      <Toolbar className={clsx(settings.isDrawerOpen && classes.toolBarShifted)}>
        <IconsBar setTheme={setTheme} />
      </Toolbar>
    </AppBar>
  )
}

const mapStateToProps = (state: AppState) => ({
  settings: state.settings.settings
})

const mapDispatchToProps = (dispatch: AppDispatch) => ({
  changeSettings: bindActionCreators(changeSettingsAction, dispatch)
})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(NavBar)
