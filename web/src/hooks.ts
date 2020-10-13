import { useState } from 'react'
import { ThemeOptions } from '@material-ui/core/styles/createMuiTheme'
import { PaletteType } from '@material-ui/core'
import { ThemeMode } from 'types'
import customTheme, { colors } from './theme'

export const useTheme = (themeMode: ThemeMode): [ThemeOptions, () => void] => {
  const overrideTheme = {
    ...customTheme,
    palette: {
      ...customTheme.palette,
      type: themeMode,
      background: {
        default: themeMode === 'light' ? colors.bgLight : colors.bgDark
      }
    }
  }
  const [theme, setTheme] = useState(overrideTheme)
  const typeTheme = theme.palette!.type!
  const toggleTheme = () => {
    const updatedTheme = {
      ...theme,
      palette: {
        ...theme.palette,
        type: (typeTheme === 'light' ? 'dark' : 'light') as PaletteType,
        background: {
          default: themeMode === 'light' ? colors.bgDark : colors.bgLight
        }
      }
    }
    setTheme(updatedTheme)
  }
  return [theme, toggleTheme]
}
