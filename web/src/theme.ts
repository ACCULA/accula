import { ThemeOptions } from '@material-ui/core/styles/createMuiTheme'

const customTheme: ThemeOptions = {
  palette: {
    type: 'light',
    background: {
      default: '#F2F2F2'
    },
    primary: {
      dark: '#222831',
      light: '#393E46',
      main: '#393E46',
      contrastText: '#fff'
    },
    secondary: {
      main: '#F96D00',
      contrastText: '#000'
    }
  }
}

export default customTheme
