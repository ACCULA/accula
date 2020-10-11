import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  tableRow: {
    textDecoration: 'none',
    backgroundColor: theme.palette.type === 'light' ? '#fff' : 'rgba(77, 83, 92, 0.5)',
    '&:hover': {
      cursor: 'pointer'
    }
  }
}))
