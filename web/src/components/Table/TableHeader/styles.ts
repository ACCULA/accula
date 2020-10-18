import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  tableHeadCell: {
    backgroundColor: theme.palette.type === 'light' ? 'rgba(211, 211, 211, 0.1)' : '#4D535C',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.3)' : '#fff',
    fontSize: '18px',
    fontWeight: 500
  },
  tableHeadRow: {
    '&:hover': {
      cursor: 'default'
    }
  }
}))
