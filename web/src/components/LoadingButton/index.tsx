import React from 'react'
import { Button, ButtonProps } from 'react-bootstrap'
import ReactLoader from 'react-loader-spinner'

interface LoadingButtonProps extends ButtonProps {
  isLoading?: boolean
}

export const LoadingButton = (props: LoadingButtonProps) => {
  const { isLoading, children, ...btnProps } = props
  return (
    <Button {...btnProps} disabled={btnProps.disabled || isLoading}>
      <ReactLoader
        type="TailSpin" //
        color="#FFF"
        height={15}
        width={15}
        visible={isLoading || false}
      />
      <div style={{ marginLeft: isLoading ? 5 : 0 }}>{children}</div>
    </Button>
  )
}
