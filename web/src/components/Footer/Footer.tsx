import React from 'react'
import { Grid } from 'react-bootstrap'

const Footer: React.FC = () => {
  return (
    <footer className="footer">
      <Grid fluid>
        <p className="copyright pull-right">
          &copy; {new Date().getFullYear()}{' '}
          ACCULA
        </p>
      </Grid>
    </footer>
  )
}

export default Footer
