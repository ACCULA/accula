import React from 'react'
import ReactSelect from 'react-select'

export const Select = (props: any) => {
  const allOption = {
    label: 'Select all',
    value: '*'
  }
  return (
    <ReactSelect
      {...props}
      options={[allOption, ...props.options]}
      onChange={(selected: any) => {
        if (selected.length > 0 && selected[selected.length - 1].value === allOption.value) {
          return props.onChange(props.options)
        }
        return props.onChange(selected)
      }}
    />
  )
}
