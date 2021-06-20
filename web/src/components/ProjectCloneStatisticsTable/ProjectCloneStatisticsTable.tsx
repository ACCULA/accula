import React from 'react'
import { ICloneStatistics } from 'types'
import { Avatar } from '@material-ui/core'
import Table from 'components/Table/Table'
import TableRow from 'components/Table/TableRow'
import TableCell from 'components/Table/TableCell'
import { useStyles } from './styles'

interface ProjectCloneStatisticsTableProps {
  cloneStatisticsItems: ICloneStatistics[]
}

const ProjectCloneStatisticsTable = ({
  cloneStatisticsItems
}: ProjectCloneStatisticsTableProps) => {
  const classes = useStyles()

  return (
    <div>
      <Table<ICloneStatistics>
        headCells={[
          { id: 'user', numeric: true, disablePadding: false, label: '#' },
          { id: 'user', numeric: false, disablePadding: false, label: 'Student' },
          { id: 'cloneCount', numeric: true, disablePadding: false, label: 'Clone count' },
          { id: 'lineCount', numeric: true, disablePadding: false, label: 'Line count' }
        ]}
        count={cloneStatisticsItems.length}
        toolBarTitle=""
        toolBarButtons={[]}
      >
        {() => (
          <>
            {cloneStatisticsItems.map((cloneStatisticsItem, index) => (
              <TableRow key={cloneStatisticsItem.user.login}>
                <TableCell align="right">
                  <span className={classes.dataText}>{index + 1}</span>
                </TableCell>
                <TableCell align="left">
                  <div className={classes.authorInfo}>
                    <Avatar
                      onClick={() => window.open(cloneStatisticsItem.user.url, '_blank')}
                      className={classes.authorAvatar}
                      src={cloneStatisticsItem.user.avatar}
                      alt={cloneStatisticsItem.user.login}
                    />
                    <span className={classes.dataText}>{cloneStatisticsItem.user.login}</span>
                  </div>
                </TableCell>
                <TableCell align="right">
                  <span className={classes.dataText}>{cloneStatisticsItem.cloneCount}</span>
                </TableCell>
                <TableCell align="right">
                  <span className={classes.dataText}>{cloneStatisticsItem.lineCount}</span>
                </TableCell>
              </TableRow>
            ))}
          </>
        )}
      </Table>
    </div>
  )
}

export default ProjectCloneStatisticsTable
