import React from 'react'
import { Grid } from 'react-bootstrap'
import { Link } from 'react-router-dom'

const Footer: React.FC = () => {
  return (
    <footer className="footer">
      <Grid fluid>
        <nav className="pull-left">
          <ul>
            <li>
              <Link to="/">Home</Link>
            </li>
          </ul>
        </nav>
        <p className="copyright pull-right">
          &copy; {new Date().getFullYear()}{' '}
          ACCULA
        </p>
      </Grid>
    </footer>
  )
}

export default Footer
