import React from 'react'
import { Button, Typography } from '@material-ui/core'
import { useHistory } from 'react-router-dom'
import { useStyles } from './styles'

const NotFound: React.FC = () => {
  const classes = useStyles()
  const history = useHistory()

  return (
    <div className={classes.root}>
      <div className={classes.notfound}>
        <Typography className={classes.header44} variant="h1">
          4<span className={classes.header0}>0</span>4
        </Typography>
        <p className={classes.notfoundText}>
          The page you are looking for might have been removed had its name changed or is
          temporarily unavailable.
        </p>
        <Button
          className={classes.homeBtn}
          variant="contained"
          color="secondary"
          onClick={() => history.push('/')}
        >
          Home page
        </Button>
      </div>
    </div>
  )
}

export default NotFound
