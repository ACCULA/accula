import React from 'react'
import { NavLink } from 'react-router-dom'

interface Breadcrumb {
  text: string
  to?: string
}

interface BreadcrumbsProps {
  breadcrumbs: Breadcrumb[]
}

export const Breadcrumbs = ({ breadcrumbs }: BreadcrumbsProps) => (
  <ol className="breadcrumb">
    {breadcrumbs.map(({ text, to }) => (
      <li key={text} className={to ? '' : 'active'}>
        {to ? <NavLink to={to}>{text}</NavLink> : text}
      </li>
    ))}
  </ol>
)
