import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  titleField: {
    display: 'flex',
    alignItems: 'center'
  },
  title: {
    flexGrow: 1,
    fontSize: 19,
    fontWeight: 500
  },
  cloneTitleText: {
    flexShrink: 0,
    fontSize: 14,
    whiteSpace: 'nowrap'
  },
  intoTitle: {
    margin: '0 4px',
    color:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.addedColor
        : colors.codeDiff.dark.addedColor,
    backgroundColor:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.addedGutterBackground
        : colors.codeDiff.dark.addedGutterBackground
  },
  fromTitle: {
    margin: '0 4px',
    color:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.removedColor
        : colors.codeDiff.dark.removedColor,
    backgroundColor:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.removedGutterBackground
        : colors.codeDiff.dark.removedGutterBackground
  },
  refreshButton: {
    boxShadow: 'none',
    '&:hover': {
      boxShadow: 'none'
    }
  }
}))
