import React from 'react'
import { Button } from 'react-bootstrap'
import cx from 'classnames'

interface CustomButtonProps {
  fill?: boolean
  simple?: boolean
  pullRight?: boolean
  block?: boolean
  round?: boolean

  [rest: string]: any
}

const CustomButton = ({ fill, simple, pullRight, round, block, ...rest }: CustomButtonProps) => {
  const btnClasses = cx({
    'btn-fill': fill,
    'btn-simple': simple,
    'pull-right': pullRight,
    'btn-block': block,
    'btn-round': round
  })

  return <Button className={btnClasses} {...rest} />
}

export default CustomButton
