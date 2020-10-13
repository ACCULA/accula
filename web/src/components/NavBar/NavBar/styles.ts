import { makeStyles, Theme } from '@material-ui/core'
import { DRAWER_WIDTH } from 'utils'

export const useStyles = makeStyles((theme: Theme) => ({
  appBar: {
    background: theme.palette.type === 'light' ? '#fff' : '#4D535C',
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen
    })
  },
  appBarShift: {
    width: `calc(100% - ${DRAWER_WIDTH}px)`,
    marginLeft: DRAWER_WIDTH,
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen
    })
  },
  toolBarShifted: {
    paddingLeft: '0px'
  }
}))
