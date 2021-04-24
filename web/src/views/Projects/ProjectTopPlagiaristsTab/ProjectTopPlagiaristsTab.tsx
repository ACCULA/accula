import React from 'react'
import { IPlagiarist, IProject } from 'types'
import EmptyContent from 'components/EmptyContent'
import { useStyles } from './styles'
import { TrendingUp } from "@material-ui/icons";
import { Avatar, TableCell, TableRow } from "@material-ui/core";
import Table from "../../../components/Table";

interface ProjectTopPlagiaristsTabProps {
  project: IProject
  topPlagiarists: IPlagiarist[]
}

const ProjectTopPlagiaristsTab = ({ topPlagiarists }: ProjectTopPlagiaristsTabProps) => {
  const classes = useStyles()

  if (!topPlagiarists) {
    return <></>
  }

  if (topPlagiarists.length === 0) {
    return <EmptyContent className={classes.emptyContent} Icon={TrendingUp} info="No plagiarists" />
  }

  return (
      <div>
        <Table<IPlagiarist>
            headCells={[
                { id: 'user', numeric: true, disablePadding: false, label: '#' },
                { id: 'user', numeric: false, disablePadding: false, label: 'Plagiarist' },
                { id: 'cloneCount', numeric: true, disablePadding: false, label: 'Clone count' },
            ]}
            count={topPlagiarists.length}
            toolBarTitle=""
            toolBarButtons={[]}
        >
          {() => (
              <>
                {topPlagiarists.map((plagiarist, index) => (
                    <TableRow
                        key={plagiarist.user.login}
                    >
                      <TableCell align="right">
                          <span className={classes.dataText}>{index + 1}</span>
                      </TableCell>
                      <TableCell align="left">
                          <div className={classes.authorInfo}>
                              <Avatar
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
                    </TableRow>
                ))}
              </>
          )}
        </Table>
      </div>
  )
}

export default ProjectTopPlagiaristsTab
