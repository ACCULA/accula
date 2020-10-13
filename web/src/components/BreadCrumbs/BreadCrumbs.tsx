import React from 'react'
import clsx from 'clsx'
import { Breadcrumbs, Typography } from '@material-ui/core'
import { Link } from 'react-router-dom'
import { useStyles } from './styles'

interface BreadCrumb {
  text: string
  to?: string
}

interface BreadCrumbsProps {
  breadcrumbs?: BreadCrumb[]
}

const BreadCrumbs = ({ breadcrumbs }: BreadCrumbsProps) => {
  const classes = useStyles()
  return (
    <Breadcrumbs className={classes.crumb} aria-label="breadcrumb">
      {breadcrumbs &&
        breadcrumbs.map(({ text, to }) =>
          to ? (
            <Link key={text} className={classes.breadcrumbLink} to={to}>
              {text}
            </Link>
          ) : (
            <Typography
              key={text}
              className={clsx(classes.crumb, classes.activeCrumb)}
              color="textPrimary"
            >
              {text}
            </Typography>
          )
        )}
    </Breadcrumbs>
  )
}

export default BreadCrumbs
