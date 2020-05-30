import React from 'react'
import { Button, ButtonProps } from 'react-bootstrap'

interface LoadingButtonProps extends ButtonProps {
  isLoading?: boolean
}

export const LoadingButton = (props: LoadingButtonProps) => {
  const { isLoading, children, ...btnProps } = props
  return (
    <Button {...btnProps} disabled={btnProps.disabled || isLoading}>
      {isLoading && <i className="fas fa-spinner fa-spin" />}
      <div style={{ marginLeft: isLoading ? 5 : 0 }}>{children}</div>
    </Button>
  )
}
