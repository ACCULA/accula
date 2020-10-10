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
  addedTitle: {
    color:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.addedColor
        : colors.codeDiff.dark.addedColor,
    backgroundColor:
      theme.palette.type === 'light'
        ? colors.codeDiff.light.addedGutterBackground
        : colors.codeDiff.dark.addedGutterBackground
  },
  removedTitle: {
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
