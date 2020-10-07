import { makeStyles, Theme } from '@material-ui/core'

export const useStyles = makeStyles((theme: Theme) => ({
  emptyContent: {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    margin: 'auto'
  },
  layersImg: {
    width: '152px',
    height: '152px'
  },
  projectsText: {
    fontSize: '38px',
    fontWeight: 500,
    marginBottom: '28px'
  },
  addProjectBtn: {
    boxShadow: 'none',
    '&:hover': {
      boxShadow: 'none'
    }
  },
  breadcrumbs: {
    marginBottom: '24px'
  },
  tabs: {
    marginBottom: '53px'
  },
  repoUrlImg: {
    width: '33px',
    height: '33px'
  },
  cellText: {
    fontSize: '18px',
    fontWeight: 500
  },
  repoInfo: {
    display: 'flex',
    alignContent: 'center'
  },
  repoFullName: {
    marginLeft: '10px',
    display: 'flex',
    justifyContent: 'center',
    flexDirection: 'column'
  },
  repoDescription: {
    color: theme.palette.type === 'light' ? 'rgba(0, 0, 0, 0.5)' : 'rgba(255, 255, 255, 0.5)'
  },
  repoAvatarImg: {
    width: '52px',
    height: '52px'
  }
}))
