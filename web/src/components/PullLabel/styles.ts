import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  label: {
    color: theme.palette.type === 'light' ? '#2178a3' : '#e3f2f9',
    background: theme.palette.type === 'light' ? '#e3f2f9' : '#2178a3',
    padding: '2px 4px'
  }
}))
