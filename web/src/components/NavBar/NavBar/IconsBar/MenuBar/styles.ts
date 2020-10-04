import { makeStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  menuItemTitle: {
    margin: 0,
    padding: 0
  },
  avatar: {
    width: '30px',
    height: '30px'
  },
  avatarBtn: {
    backgroundColor: 'transparent',
    '&:hover': {
      backgroundColor: 'transparent'
    }
  }
}))
