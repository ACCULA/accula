import React from 'react'
import { Helmet } from 'react-helmet'

interface PageTitleProps {
  title?: string
}

export const PageTitle = ({ title }: PageTitleProps) => (
  <Helmet>
    {title ? ( //
      <title>{`${title} - ACCULA`}</title>
    ) : (
      <title>ACCULA</title>
    )}
  </Helmet>
)
