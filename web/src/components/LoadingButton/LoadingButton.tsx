import React from 'react'
import clsx from 'clsx'
import { CircularProgress, Button } from '@material-ui/core'

import { useStyles } from './styles'

interface LoadingButtonProps {
  text: string
  submitting: boolean
  disabled?: boolean
  onClick?: () => void
  className?: string
}

const LoadingButton = ({ text, disabled, submitting, onClick, className }: LoadingButtonProps) => {
  const classes = useStyles()

  return (
    <div className={classes.buttonWrapper}>
      <Button
        className={clsx(className)}
        type="submit"
        variant="contained"
        color="secondary"
        disabled={disabled || submitting}
        onClick={onClick}
      >
        {text}
      </Button>
      {submitting && (
        <CircularProgress color="secondary" size={24} className={classes.buttonProgress} />
      )}
    </div>
  )
}

export default LoadingButton
