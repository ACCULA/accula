import { makeStyles, PaletteType } from '@material-ui/core'

export const useStyles = (theme: PaletteType): Record<'toolBar' | 'appBar', string> => {
  const styles = makeStyles(() => ({
    appBar: {
      background: theme === 'light' ? '#fff' : '#4D535C'
    },
    toolBar: {
      display: 'flex',
      justifyContent: 'flex-end'
    }
  }))
  return styles()
}
