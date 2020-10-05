import { makeStyles, PaletteType, Theme } from '@material-ui/core'
import { drawerWidth } from 'utils'

export const useStyles = (
  theme: PaletteType
): Record<'appBar' | 'appBarShift' | 'toolBarShifted', string> => {
  const styles = makeStyles(({ transitions, breakpoints }: Theme) => ({
    appBar: {
      background: theme === 'light' ? '#fff' : '#4D535C',
      transition: transitions.create(['width', 'margin'], {
        easing: transitions.easing.sharp,
        duration: transitions.duration.leavingScreen
      })
    },
    appBarShift: {
      width: `calc(100% - ${drawerWidth}px)`,
      marginLeft: drawerWidth,
      transition: transitions.create(['width', 'margin'], {
        easing: transitions.easing.sharp,
        duration: transitions.duration.enteringScreen
      })
    },
    toolBarShifted: {
      paddingLeft: '0px'
    }
  }))
  return styles()
}
