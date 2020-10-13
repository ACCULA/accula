import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  root: {
    width: '100%'
  },
  paper: {
    width: '100%',
    marginBottom: theme.spacing(3)
  },
  table: {
    minWidth: 750
  },
  pagination: {
    backgroundColor: theme.palette.type === 'light' ? 'rgba(211, 211, 211, 0.1)' : '#4D535C'
  },
  tableRow: {
    textDecoration: 'none',
    backgroundColor: theme.palette.type === 'light' ? '#fff' : 'rgba(77, 83, 92, 0.5)',
    '&:hover': {
      cursor: 'pointer'
    }
  }
}))
