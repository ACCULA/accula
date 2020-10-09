import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  menu: {
    marginTop: 8
  },
  menuItemTitle: {
    margin: 0,
    padding: 0
  },
  avatar: {
    width: 30,
    height: 30
  },
  avatarBtn: {
    backgroundColor: theme.palette.type === 'light' ? colors.bgLight : colors.bgDark,
    padding: 2,
    borderRadius: 23
  },
  avatarBtnText: {
    marginLeft: 8,
    fontSize: 14,
    fontWeight: 700,
    [theme.breakpoints.down('sm')]: {
      display: 'none'
    }
  }
}))
