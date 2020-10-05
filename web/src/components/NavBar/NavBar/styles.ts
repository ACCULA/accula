import { makeStyles, Theme } from '@material-ui/core'
import { drawerWidth } from 'utils'

export const useStyles = makeStyles((theme: Theme) => ({
  appBar: {
    background: theme.palette.type === 'light' ? '#fff' : '#4D535C',
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen
    })
  },
  appBarShift: {
    width: `calc(100% - ${drawerWidth}px)`,
    marginLeft: drawerWidth,
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen
    })
  },
  toolBarShifted: {
    paddingLeft: '0px'
  }
}))
