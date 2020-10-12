import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  panel: {
    marginBottom: 16,
    fontSize: 12,
    borderRadius: 8
  },
  panelHeader: {
    flexDirection: 'row-reverse',
    backgroundColor: theme.palette.type === 'light' ? 'rgba(211, 211, 211, 0.1)' : '#2c313a',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.3)' : '#fff'
  },
  panelHeaderContent: {
    marginLeft: '12px !important'
  },
  panelData: {
    padding: 0,
    overflowX: 'auto'
  }
}))

export const codeDiffStyles = {
  variables: colors.codeDiff
}
