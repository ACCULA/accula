import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  tabs: {
    marginBottom: '64px',
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
  tab: {
    textTransform: 'none',
    fontSize: '1rem'
  },
  tabContent: {
    display: 'flex',
    alignItems: 'center'
  },
  tabImg: {
    width: '16px',
    height: '16px',
    marginRight: '6px',
    fill: theme.palette.type === 'light' ? colors.primaryLight : '#fff'
  },
  badge: {
    paddingRight: `${theme.spacing(1)}px`
  },
  dummyTab: {
    display: 'none'
  }
}))
