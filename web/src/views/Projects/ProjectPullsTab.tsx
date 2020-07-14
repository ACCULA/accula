import React from 'react'
import { Badge, Panel, Table } from 'react-bootstrap'
import { LinkContainer } from 'react-router-bootstrap'
import { Wrapper } from 'store/wrapper'
import { IProject, IShortPull } from 'types'
import { Loader } from 'components/Loader'

interface ProjectPullsTabProps {
  project: Wrapper<IProject>
  pulls: Wrapper<IShortPull[]>
}

export const ProjectPullsTab = ({
  project, //
  pulls
}: ProjectPullsTabProps) => {
  return project.isFetching || !project.value || pulls.isFetching || !pulls.value ? (
    <Loader />
  ) : (
    <Panel className="project panel-project">
      <Table striped bordered hover responsive>
        <thead>
          <tr className="project-pull">
            <th className="id">#</th>
            <th>Pull Request</th>
            <th>Author</th>
          </tr>
        </thead>
        <tbody>
          {pulls.value.map(pull => (
            <LinkContainer
              to={`/projects/${project.value.id}/pulls/${pull.number}`}
              key={pull.number}
            >
              <tr className="project-pull pointer">
                <td className="id">{pull.number}</td>
                <td>
                  {pull.open ? (
                    <Badge className="badge-success">Open</Badge> //
                  ) : (
                    <Badge className="badge-danger">Closed</Badge>
                  )}
                  {pull.title}
                </td>
                <td className="avatar">
                  <img className="border-gray" src={pull.author.avatar} alt={pull.author.login} />
                  {pull.author.login}
                </td>
              </tr>
            </LinkContainer>
          ))}
        </tbody>
      </Table>
    </Panel>
  )
}
