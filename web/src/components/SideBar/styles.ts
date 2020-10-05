import { makeStyles, PaletteType, Theme } from '@material-ui/core'
import { colors } from 'theme'
import { drawerWidth } from 'utils'

// TODO: Simplify the type of return value
export const useStyles = (
  theme: PaletteType
): Record<
  | 'drawer'
  | 'drawerPaper'
  | 'drawerHeader'
  | 'logo'
  | 'logoImg'
  | 'logoText'
  | 'itemText'
  | 'activeItem'
  | 'itemList'
  | 'drawerBottom',
  string
> => {
  const styles = makeStyles(({ mixins }: Theme) => ({
    drawer: {
      width: drawerWidth,
      flexShrink: 0
    },
    drawerPaper: {
      width: drawerWidth,
      background: theme === 'light' ? colors.primaryLight : colors.primaryDark,
      color: '#fff'
    },
    drawerHeader: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      paddingTop: '8px',
      paddingRight: '8px',
      paddingLeft: '8px',
      ...mixins.toolbar
    },
    logo: {
      textDecoration: 'none'
    },
    logoImg: {
      width: '42px',
      height: '42px'
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
      backgroundColor: `${theme === 'light' ? colors.primaryDark : colors.primaryLight} !important`,
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
  return styles()
}
