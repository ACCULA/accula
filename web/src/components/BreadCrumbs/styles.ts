import { makeStyles, Theme } from '@material-ui/core'
import { colors } from 'theme'

export const useStyles = makeStyles((theme: Theme) => ({
  crumb: {
    fontSize: 38,
    fontWeight: 500,
    marginBottom: 24,
    [theme.breakpoints.down('xs')]: {
      fontSize: 28
    }
  },
  breadcrumbLink: {
    textDecoration: 'none',
    color: colors.secondaryLight
  },
  activeCrumb: {
    marginBottom: 0
  }
}))
