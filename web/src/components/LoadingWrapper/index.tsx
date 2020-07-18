import React, { PropsWithChildren } from 'react'
import { Loader } from 'components/Loader'
import { Wrapper } from 'store/wrapper'
import { Alert } from 'react-bootstrap'

interface LoadingWrapperProps {
  deps: Wrapper<any>[]
}

export const LoadingWrapper: React.FC<LoadingWrapperProps> = ({
  deps,
  children
}: PropsWithChildren<LoadingWrapperProps>) => {
  const isFetching = deps.some(v => v.isFetching || v.value === undefined)
  const errors = deps.map(v => v.error).filter(e => e !== undefined)
  return isFetching ? (
    <Loader />
  ) : errors.length > 0 ? (
    <Alert bsStyle="danger">{errors.join('\n')}</Alert>
  ) : (
    <>{children}</>
  )
}
