import { makeStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  tabs: {
    position: 'relative',
    '&::after': {
      content: '""',
      width: '100%',
      height: 0,
      position: 'absolute',
      borderBottom: '3px solid #C3C3C3',
      top: '95%',
      zIndex: '-1'
    }
  },
  dummyTab: {
    display: 'none'
  }
}))
