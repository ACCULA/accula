import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  panel: {
    marginBottom: 16,
    overflowX: 'auto',
    fontSize: 12,
    borderRadius: 8
  },
  panelHeader: {
    backgroundColor: theme.palette.type === 'light' ? 'rgba(211, 211, 211, 0.1)' : '#2c313a',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.3)' : '#fff'
  },
  panelData: {
    padding: 0
  }
}))

export const codeDiffStyles = {
  variables: colors.codeDiff
}
