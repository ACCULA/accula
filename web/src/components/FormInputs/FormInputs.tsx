import React from 'react'
import { ControlLabel, FormControl, FormGroup, Row } from 'react-bootstrap'

const FieldGroup = ({ label, ...props }) => {
  return (
    <FormGroup>
      <ControlLabel>{label}</ControlLabel>
      <FormControl {...props} />
    </FormGroup>
  )
}

interface FormInputsProps {
  ncols: string[]
  properties: any[]
}

export const FormInputs = (props: FormInputsProps) => {
  const row = []
  for (let i = 0; i < props.ncols.length; i++) {
    row.push(
      <div key={i} className={props.ncols[i]}>
        <FieldGroup {...props.properties[i]} />
      </div>
    )
  }
  return <Row>{row}</Row>
}

export default FormInputs
