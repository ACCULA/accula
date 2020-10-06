import { createStyles, makeStyles, TableCell, Theme, withStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  visuallyHidden: {
    border: 0,
    clip: 'rect(0 0 0 0)',
    height: 1,
    margin: -1,
    overflow: 'hidden',
    padding: 0,
    position: 'absolute',
    top: 20,
    width: 1
  }
}))

export const StyledTableCell = withStyles((theme: Theme) =>
  createStyles({
    head: {
      backgroundColor: theme.palette.type === 'light' ? 'rgba(211, 211, 211, 0.1)' : '#4D535C',
      color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.3)' : '#fff',
      fontSize: '18px',
      fontWeight: 500
    }
  })
)(TableCell)
