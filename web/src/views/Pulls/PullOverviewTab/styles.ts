import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  pullOverview: {
    display: 'flex',
    alignItems: 'center',
    margin: '20px 0',
    [theme.breakpoints.down('xs')]: {
      flexDirection: 'column'
    }
  },
  authorAvatar: {
    width: 192,
    height: 192,
    boxShadow: '0 4px 60px rgba(0,0,0,.3)'
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
  authorLoginField: {
    display: 'flex',
    alignItems: 'center',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.54);' : '#fff',
    textDecoration: 'none',
    marginTop: 20
  },
  authorLogin: {
    fontWeight: 700,
    marginLeft: 8
  },
  pullInfo: {
    display: 'flex',
    flexDirection: 'column',
    alignSelf: 'flex-end',
    marginLeft: 20,
    [theme.breakpoints.down('xs')]: {
      margin: 0,
      alignSelf: 'center'
    }
  },
  pullTitle: {
    marginBottom: 8,
    marginTop: 0,
    fontSize: 48,
    [theme.breakpoints.down('xs')]: {
      fontSize: '2em',
      textAlign: 'center',
      marginTop: 10
    }
  },
  prImage: {
    width: 16,
    height: 16,
    minWidth: 16,
    minHeight: 16,
    marginRight: 6,
    fill: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.7)' : '#fff'
  },
  pullInfoField: {
    display: 'flex',
    alignItems: 'end',
    marginBottom: 4,
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.7)' : '#fff',
    fontSize: '0.9rem'
  },
  pullStatus: {
    marginLeft: 6,
    width: 14,
    height: 14
  },
  dateField: {
    marginLeft: 4
  },
  githubLink: {
    color: theme.palette.type === 'light' ? '#2178a3' : '#e3f2f9',
    background: theme.palette.type === 'light' ? '#e3f2f9' : '#2178a3',
    padding: '1px 2px'
  },
  pullRequestsBlock: {
    marginTop: 64
  },
  titleOfBlock: {
    fontSize: 24,
    fontWeight: 400
  }
}))
