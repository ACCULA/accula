import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  root: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100vh'
  },
  notfound: {
    display: 'flex',
    flexDirection: 'column',
    maxWidth: '460px',
    width: '100%',
    textAlign: 'center',
    lineHeight: 1.4
  },
  header44: {
    height: '158px',
    lineHeight: '153px',
    color: theme.palette.type === 'light' ? colors.bgLight : colors.bgDark,
    fontSize: '15rem',
    [theme.breakpoints.down('sm')]: {
      fontSize: '10rem'
    },
    letterSpacing: '10px',
    margin: '0px',
    fontWeight: 800,
    textShadow: '4px 4px 0px #6fc2d0, -4px -4px 0px #6fc2d0'
  },
  header0: {
    textShadow: `4px 4px 0px ${colors.secondaryLight}, -4px -4px 0px ${colors.secondaryLight}, 0px 0px 16px ${colors.secondaryLight}`
  },
  notfoundText: {
    color: theme.palette.type === 'light' ? '#666' : '#fff',
    fontSize: '16px',
    fontWeight: 400,
    marginTop: '15px',
    marginBottom: '30px'
  },
  homeBtn: {
    margin: 'auto',
    textAlign: 'center'
  }
}))
