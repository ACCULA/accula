import { makeStyles } from '@material-ui/core'

export const useStyles = makeStyles(() => ({
  blob: {
    borderRadius: '50%',
    marginRight: '10px',
    height: '20px',
    width: '20px',
    transform: 'scale(1)'
  },
  blobRed: {
    background: 'rgba(255, 82, 82, 1)',
    boxShadow: '0 0 0 0 rgba(255, 82, 82, 1)'
  },
  blobGreen: {
    background: 'rgba(51, 217, 178, 1)',
    boxShadow: '0 0 0 0 rgba(51, 217, 178, 1)'
  }
}))
