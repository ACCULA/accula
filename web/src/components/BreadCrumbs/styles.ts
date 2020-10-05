import { makeStyles } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles(() => ({
  root: {
    fontSize: '38px',
    fontWeight: 500
  },
  breadcrumbLink: {
    textDecoration: 'none',
    color: colors.secondaryLight
  },
  activeCrumb: {
    fontSize: '38px',
    fontWeight: 500
  }
}))
