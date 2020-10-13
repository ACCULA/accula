import React from 'react'
import { AppState } from 'store'
import { connect, ConnectedProps } from 'react-redux'
import { Avatar, IconButton } from '@material-ui/core'
import { GitHub } from '@material-ui/icons'
import Link from 'components/Link'
import { useStyles } from './styles'

interface UserProfileProps extends PropsFromRedux {}

const UserProfile = ({ user }: UserProfileProps) => {
  const classes = useStyles()

  if (!user) {
    return <></>
  }

  return (
    <div>
      <div className={classes.userOverview}>
        <Avatar className={classes.userAvatar} src={user.avatar} />
        <div className={classes.userInfo}>
          <h1 className={classes.userName}>{user.name}</h1>
          <Link className={classes.userLoginField} to={`https://github.com/${user.login}`}>
            <IconButton
              className={classes.githubButton}
              color="default"
              aria-label="Log in with GitHub"
            >
              <GitHub />
            </IconButton>
            <span className={classes.userLogin}>{`@${user.login}`}</span>
          </Link>
        </div>
      </div>
    </div>
  )
}

const mapStateToProps = (state: AppState) => ({
  user: state.users.user.value
})

const connector = connect(mapStateToProps, null)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(UserProfile)
