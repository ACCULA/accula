import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'
import { DRAWER_WIDTH } from 'utils'

export const useStyles = makeStyles((theme: Theme) => ({
  drawer: {
    width: DRAWER_WIDTH,
    flexShrink: 0
  },
  drawerPaper: {
    width: DRAWER_WIDTH,
    background: theme.palette.type === 'light' ? colors.primaryLight : colors.primaryDark,
    color: '#fff'
  },
  drawerHeader: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: '8px',
    paddingRight: '8px',
    paddingLeft: '8px',
    ...theme.mixins.toolbar
  },
  logo: {
    textDecoration: 'none'
  },
  logoImg: {
    width: '42px',
    height: '42px',
    fill: colors.secondaryLight
  },
  logoText: {
    color: '#fff',
    fontFamily: 'Righteous, Helvetica, Arial, cursive',
    fontSize: '40px'
  },
  itemList: {
    paddingTop: '0px'
  },
  activeItem: {
    backgroundColor: `${
      theme.palette.type === 'light' ? colors.primaryDark : colors.primaryLight
    } !important`,
    opacity: '1 !important',
    '&::after': {
      content: '""',
      width: 0,
      height: '100%',
      position: 'absolute',
      border: `3px solid ${colors.secondaryLight}`,
      top: '0',
      left: '253px'
    }
  },
  itemText: {
    fontSize: '1rem',
    fontWeight: 700
  },
  drawerBottom: {
    marginTop: 'auto',
    marginBottom: '44px',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  }
}))
