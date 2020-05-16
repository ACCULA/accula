import React, { PropsWithChildren } from 'react'
import cx from 'classnames'

interface CardProps {
  title: string
  category?: string
  plain?: boolean
  hCenter?: boolean
  ctAllIcons?: boolean
  ctTableFullWidth?: boolean
  ctTableResponsive?: boolean
  ctTableUpgrade?: boolean
  legend?: string
  footerIcon?: string
  footer?: string
}

const Card = ({
  ctTableFullWidth,
  ctTableResponsive,
  ctTableUpgrade,
  ctAllIcons,
  children,
  plain,
  footer,
  category,
  title,
  legend,
  hCenter,
  footerIcon
}: PropsWithChildren<CardProps>) => {
  const ctClasses = cx({
    'all-icons': ctAllIcons,
    'table-full-width': ctTableFullWidth,
    'table-responsive': ctTableResponsive,
    'table-upgrade': ctTableUpgrade
  })
  return (
    <div className={`card ${plain ? 'card-plain' : ''}`}>
      <div className={`header${hCenter ? ' text-center' : ''}`}>
        <h4 className="title">{title}</h4>
        {category && <p className="category">{category}</p>}
      </div>
      <div className={`content ${ctClasses}`}>
        {children}
        <div className="footer">
          {legend}
          {footer != null ? <hr /> : ''}
          <div className="stats">
            <i className={`fa fa-fw fa-${footerIcon}`} /> {footer}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Card
