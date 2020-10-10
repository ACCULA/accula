import React from 'react'
import ReactSelect from 'react-select'

export const Select = (props: any) => {
  const allOption = {
    label: 'Select all',
    value: '*'
  }
  const options = props.options && props.options.length > 0 ? [allOption, ...props.options] : []
  return (
    <ReactSelect
      {...props}
      options={options}
      onChange={(selected: any) => {
        if (
          selected &&
          selected.length > 0 &&
          selected[selected.length - 1].value === allOption.value
        ) {
          return props.onChange(props.options)
        }
        return props.onChange(selected)
      }}
    />
  )
}
