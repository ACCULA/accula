import { useState } from 'react'
import { ThemeOptions } from '@material-ui/core/styles/createMuiTheme'
import { PaletteType } from '@material-ui/core'
import { ThemeMode } from 'types'
import customTheme from './theme'

export const useTheme = (themeMode: ThemeMode): [ThemeOptions, () => void] => {
  const overrideTheme = {
    ...customTheme,
    palette: {
      ...customTheme.palette,
      type: themeMode,
      background: {
        default: themeMode === 'light' ? '#F2F2F2' : '#393E46'
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
          default: typeTheme === 'light' ? '#393E46' : '#F2F2F2'
        }
      }
    }
    setTheme(updatedTheme)
  }
  return [theme, toggleTheme]
}
