import React from 'react'
import { Grid } from 'react-bootstrap'
import { Link } from 'react-router-dom'

const Footer: React.FC = () => {
  return (
    <footer className="footer">
      <Grid fluid>
        <p className="copyright pull-right">
          &copy; {new Date().getFullYear()} <Link to="/">ACCULA</Link>
        </p>
      </Grid>
    </footer>
  )
}

export default Footer
