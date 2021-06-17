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

const ProjectCloneStatisticsTable = ({ cloneStatisticsItems }: ProjectCloneStatisticsTableProps) => {
  const classes = useStyles()

  return (
      <div>
        <Table<ICloneStatistics>
            headCells={[
              { id: 'user', numeric: true, disablePadding: false, label: '#' },
              { id: 'user', numeric: false, disablePadding: false, label: 'Student' },
              { id: 'cloneCount', numeric: true, disablePadding: false, label: 'Clone count' },
              { id: 'lineCount', numeric: true, disablePadding: false, label: 'Line count' },
            ]}
            count={cloneStatisticsItems.length}
            toolBarTitle=""
            toolBarButtons={[]}
        >
          {() => (
              <>
                {cloneStatisticsItems.map((plagiarist, index) => (
                    <TableRow
                        key={plagiarist.user.login}
                    >
                      <TableCell align="right">
                        <span className={classes.dataText}>{index + 1}</span>
                      </TableCell>
                      <TableCell align="left">
                        <div className={classes.authorInfo}>
                          <Avatar
                              onClick={() => window.open(plagiarist.user.url, '_blank')}
                              className={classes.authorAvatar}
                              src={plagiarist.user.avatar}
                              alt={plagiarist.user.login}
                          />
                          <span className={classes.dataText}>{plagiarist.user.login}</span>
                        </div>
                      </TableCell>
                      <TableCell align="right">
                        <span className={classes.dataText}>{plagiarist.cloneCount}</span>
                      </TableCell>
                      <TableCell align="right">
                        <span className={classes.dataText}>{plagiarist.lineCount}</span>
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
