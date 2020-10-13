import { ThemeOptions } from '@material-ui/core/styles/createMuiTheme'
import { IColors } from './types'

export const colors = {
  bgLight: '#F2F2F2',
  bgDark: '#393E46',
  primaryLight: '#393E46',
  primaryDark: '#222831',
  secondaryLight: '#F96D00',
  secondaryDark: '#F96D00',
  codeDiff: {
    light: {
      diffViewerBackground: '#fff',
      diffViewerColor: '#212529',
      addedBackground: '#e6ffed',
      addedColor: '#24292e',
      removedBackground: '#ffeef0',
      removedColor: '#24292e',
      wordAddedBackground: '#acf2bd',
      wordRemovedBackground: '#fdb8c0',
      addedGutterBackground: '#cdffd8',
      removedGutterBackground: '#ffdce0',
      gutterBackground: '#f7f7f7',
      gutterBackgroundDark: '#f3f1f1',
      highlightBackground: '#fffbdd',
      highlightGutterBackground: '#fff5b1',
      codeFoldGutterBackground: '#dbedff',
      codeFoldBackground: '#f1f8ff',
      emptyLineBackground: '#fafbfc',
      gutterColor: '#212529',
      addedGutterColor: '#212529',
      removedGutterColor: '#212529',
      codeFoldContentColor: '#212529',
      diffViewerTitleBackground: '#fafbfc',
      diffViewerTitleColor: '#212529',
      diffViewerTitleBorderColor: '#eee'
    },
    dark: {
      diffViewerBackground: '#24292e',
      diffViewerColor: '#fff',
      addedBackground: '#164707',
      addedColor: 'white',
      removedBackground: '#632F34',
      removedColor: 'white',
      wordAddedBackground: '#23691b',
      wordRemovedBackground: '#7d383f',
      addedGutterBackground: '#164707',
      removedGutterBackground: '#632b30',
      gutterBackground: '#24292e',
      gutterBackgroundDark: '#262933',
      highlightBackground: '#2a3967',
      highlightGutterBackground: '#2d4077',
      codeFoldGutterBackground: '#1b1d23',
      codeFoldBackground: '#1b1d23',
      emptyLineBackground: '#363946',
      gutterColor: '#464c67',
      addedGutterColor: '#8c8c8c',
      removedGutterColor: '#8c8c8c',
      codeFoldContentColor: '#abb2bf',
      diffViewerTitleBackground: '#2f323e',
      diffViewerTitleColor: '#555a7b',
      diffViewerTitleBorderColor: '#353846'
    }
  }
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
