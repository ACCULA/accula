import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  emptyContent: {
    marginTop: '110px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center'
  },
  prImage: {
    width: '172px',
    height: '236px',
    fill: colors.secondaryLight
  },
  prText: {
    fontSize: '30px',
    fontWeight: 500,
    marginBottom: '28px'
  },
  dataText: {
    fontSize: '14px',
    fontWeight: 500
  },
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
