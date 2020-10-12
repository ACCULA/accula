import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  label: {
    color: theme.palette.type === 'light' ? '#2178a3' : '#e3f2f9',
    background: theme.palette.type === 'light' ? '#e3f2f9' : '#2178a3',
    padding: '2px 4px',
    overflow: 'hidden',
    maxWidth: 400,
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis'
  },
  addedLabel: {
    color:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.addedColor
        : colors.codeDiff.dark.addedColor,
    backgroundColor:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.addedGutterBackground
        : colors.codeDiff.dark.addedGutterBackground
  },
  removedLabel: {
    color:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.removedColor
        : colors.codeDiff.dark.removedColor,
    backgroundColor:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.removedGutterBackground
        : colors.codeDiff.dark.removedGutterBackground
  }
}))
