import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  panel: {
    margin: 16,
    fontSize: 12,
    borderRadius: 8,
    border: theme.palette.type === 'light' ? '1px solid #e1e4e8' : 'none'
  },
  root: {
    backgroundColor: theme.palette.type === 'light' ? '#fbfbfb !important' : '#2c313a !important',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.5)' : '#fff'
  },
  panelHeader: {
    borderBottom: theme.palette.type === 'light' ? '1px solid #e1e4e8' : 'none',
    backgroundColor: theme.palette.type === 'light' ? '#fbfbfb !important' : '#2c313a !important',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.5)' : '#fff',
    borderBottomLeftRadius: 0,
    borderBottomRightRadius: 0
  },
  panelHeaderContent: {
    overflow: 'hidden',
    flexWrap: 'wrap',
    paddingBottom: '16px !important'
  },
  disabledHeader: {
    opacity: '1 !important'
  },
  expandIcon: {
    marginRight: 0
  },
  panelData: {
    padding: 0,
    overflowX: 'auto'
  }
}))

export const codeDiffStyles = {
  variables: colors.codeDiff
}
