import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  root: {},
  userOverview: {
    display: 'flex',
    alignItems: 'center',
    margin: '20px 0',
    [theme.breakpoints.down('xs')]: {
      flexDirection: 'column'
    }
  },
  userAvatar: {
    width: 192,
    height: 192,
    boxShadow: '0 4px 60px rgba(0,0,0,.3)'
  },
  userInfo: {
    display: 'flex',
    flexDirection: 'column',
    alignSelf: 'flex-end',
    marginLeft: 20,
    [theme.breakpoints.down('xs')]: {
      margin: 0,
      alignSelf: 'center',
      alignItems: 'center'
    }
  },
  userName: {
    marginBottom: 8,
    marginTop: 0,
    fontSize: 72,
    [theme.breakpoints.down('md')]: {
      fontSize: 48
    },
    [theme.breakpoints.down('xs')]: {
      textAlign: 'center',
      marginTop: 10
    }
  },
  userLogin: {
    fontSize: '1.2rem',
    fontWeight: 700,
    marginLeft: 8
  },
  authorView: {
    marginRight: 24,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    [theme.breakpoints.down('xs')]: {
      margin: 0
    }
  },
  githubButton: {
    width: 24,
    height: 24
  },
  userLoginField: {
    display: 'flex',
    alignItems: 'center',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.54);' : '#fff',
    textDecoration: 'none',
    marginTop: 10
  }
}))
