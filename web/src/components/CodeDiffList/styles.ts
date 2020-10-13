import { makeStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  titleField: {
    display: 'flex',
    alignItems: 'center'
  },
  title: {
    flexGrow: 1,
    fontSize: 19,
    fontWeight: 500
  },
  codeList: {
    outline: 'none'
  }
}))
