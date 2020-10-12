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
  chip: {
    marginRight: 5
  },
  option: {
    display: 'flex',
    alignItems: 'center'
  },
  optionText: {
    margin: '0 5px',
    fontSize: 16,
    fontWeight: 400
  },
  avatarOption: {
    width: 30,
    height: 30
  },
  compareWithField: {
    marginBottom: 64,
    margin: '0 auto',
    maxWidth: 1000
  }
}))
