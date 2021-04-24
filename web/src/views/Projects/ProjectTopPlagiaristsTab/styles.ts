import { makeStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  emptyContent: {
    marginTop: '110px'
  },
  dataText: {
    fontSize: '14px',
    fontWeight: 500
  },
  authorInfo: {
    display: 'flex',
    alignItems: 'center'
  },
  authorAvatar: {
    width: '34px',
    height: '34px',
    marginRight: '10px'
  }
}))
