import React from 'react'
import IconButton from '@material-ui/core/IconButton'
import { connect, ConnectedProps } from 'react-redux'
import { AppState } from 'store'
import { useHistory } from 'react-router-dom'
import { Menu, MenuItem, ListItemIcon, Avatar } from '@material-ui/core'
import { AccountCircle, ArrowDropDownRounded } from '@material-ui/icons'
import { historyPush } from 'utils'
import { useStyles } from './styles'

interface MenuBarProps extends PropsFromRedux {}

const MenuBar: React.FC<MenuBarProps> = ({ user }: MenuBarProps) => {
  const history = useHistory()
  const classes = useStyles()
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)
  const isMenuOpen = Boolean(anchorEl)

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleClickListItem = (onClick?: () => void) => {
    if (onClick) {
      onClick()
    }
    handleMenuClose()
  }

  const menuId = 'more-actions-menu'
  const renderMenu = (
    <Menu
      anchorEl={anchorEl}
      getContentAnchorEl={null}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      id={menuId}
      keepMounted
      transformOrigin={{ vertical: 'top', horizontal: 'center' }}
      open={isMenuOpen}
      onClose={handleMenuClose}
    >
      <MenuItem onClick={() => handleClickListItem(() => historyPush(history, '/profile'))}>
        <ListItemIcon aria-label="Account of current user" color="inherit">
          <AccountCircle />
        </ListItemIcon>
        <p className={classes.menuItemTitle}>Profile</p>
      </MenuItem>
    </Menu>
  )

  return (
    <>
      <IconButton
        edge="end"
        aria-label="display more actions"
        aria-controls={menuId}
        aria-haspopup="true"
        onClick={handleMenuOpen}
        className={classes.avatarBtn}
      >
        <Avatar alt={user.name} src={user.avatar} className={classes.avatar} />
        <ArrowDropDownRounded />
      </IconButton>
      {renderMenu}
    </>
  )
}

const mapStateToProps = (state: AppState) => ({
  user: state.users.user.value
})

const mapDispatchToProps = () => ({})

const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(MenuBar)
