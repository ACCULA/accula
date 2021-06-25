import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  root: {
    maxWidth: 1000,
    margin: '0 auto'
  },
  card: {
    marginBottom: 40
  },
  title: {
    fontSize: 24,
    fontWeight: 400
  },
  option: {
    display: 'flex',
    alignItems: 'center'
  },
  avatarOption: {
    width: 30,
    height: 30
  },
  optionText: {
    marginLeft: 10,
    fontSize: 16,
    fontWeight: 400
  },
  chip: {
    marginRight: 5
  },
  description: {
    marginTop: 10,
    marginBottom: 20,
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.5)' : '#fff',
    fontSize: '0.9rem'
  },
  saveButtonContainer: {
    display: 'flex',
    flexDirection: 'row-reverse'
  }
}))
