import { createStyles, makeStyles, TableRow, Theme, withStyles } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  root: {
    width: '100%'
  },
  paper: {
    width: '100%',
    marginBottom: theme.spacing(2)
  },
  table: {
    minWidth: 750
  }
}))

export const StyledTableRow = withStyles((theme: Theme) =>
  createStyles({
    root: {
      backgroundColor: theme.palette.type === 'light' ? '#fff' : 'rgba(77, 83, 92, 0.5)',
      '&:hover': {
        cursor: 'pointer'
      }
    }
  })
)(TableRow)
