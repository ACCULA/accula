import { ThemeOptions } from '@material-ui/core/styles/createMuiTheme'
import { IColors } from './types'

export const colors = {
  bgLight: '#F2F2F2',
  bgDark: '#393E46',
  primaryLight: '#393E46',
  primaryDark: '#222831',
  secondaryLight: '#F96D00',
  secondaryDark: '#F96D00'
} as IColors

const customTheme: ThemeOptions = {
  palette: {
    type: 'light',
    background: {
      default: colors.bgLight
    },
    primary: {
      dark: colors.primaryDark,
      light: colors.primaryLight,
      main: colors.primaryLight,
      contrastText: '#fff'
    },
    secondary: {
      dark: colors.secondaryLight,
      light: colors.secondaryLight,
      main: colors.secondaryLight,
      contrastText: '#fff'
    }
  }
}

export default customTheme
