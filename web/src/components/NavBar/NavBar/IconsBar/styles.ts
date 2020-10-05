import { makeStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  menuButton: {
    marginRight: 36
  },
  hide: {
    display: 'none'
  },
  mainTools: {
    marginLeft: 'auto'
  },
  login: {
    color: 'inherit',
    '&:hover': {
      textDecoration: 'none',
      color: 'inherit'
    }
  }
}))
