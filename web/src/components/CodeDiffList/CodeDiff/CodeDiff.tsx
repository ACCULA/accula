import React from 'react'
import { Accordion, AccordionDetails, Card, CardContent } from '@material-ui/core'
import Prism from 'prismjs'
import ReactDiffViewer, { ReactDiffViewerProps } from '../ReactDiffViewer'
import 'prismjs/components/prism-java'
import { codeDiffStyles, useStyles } from './styles'

interface CodeDiffProps extends ReactDiffViewerProps {
  language: string
  title: React.ReactNode
  defaultExpanded?: boolean
}

const CodeDiff = ({ title, language, defaultExpanded, ...props }: CodeDiffProps) => {
  const classes = useStyles()

  const highlight = (code: string): JSX.Element => {
    if (!language || code === undefined) {
      return <>{code}</>
    }
    if (typeof code === 'undefined') {
      return <></>
    }
    return (
      <pre
        style={{ display: 'inline' }}
        /* eslint-disable-next-line react/no-danger */
        dangerouslySetInnerHTML={{
          __html: Prism.highlight(code, Prism.languages[language], language)
        }}
      />
    )
  }

  return (
    <>
      <Accordion classes={{ expanded: classes.panel, root: classes.root }} disabled defaultExpanded>
        <Card className={classes.panelHeader}>
          <CardContent className={classes.panelHeaderContent}>
            {title}
          </CardContent>
        </Card>
        <AccordionDetails className={classes.panelData}>
          <ReactDiffViewer
            styles={props.styles || codeDiffStyles}
            renderContent={props.renderContent || highlight}
            {...props}
          />
        </AccordionDetails>
      </Accordion>
    </>
  )
}

export default CodeDiff
