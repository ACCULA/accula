import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  root: {
    minWidth: 275
  },
  title: {
    fontSize: 20,
    fontWeight: 500
  },
  option: {
    display: 'flex',
    alignItems: 'center'
  },
  optionText: {
    marginLeft: '10px',
    fontSize: 16,
    fontWeight: 400
  },
  chip: {
    marginRight: 5
  },
  description: {
    marginTop: '10px',
    marginBottom: '20px',
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.5)' : '#fff',
    fontSize: '0.9rem'
  },
  saveButtonContainer: {
    display: 'flex',
    flexDirection: 'row-reverse'
  }
}))
