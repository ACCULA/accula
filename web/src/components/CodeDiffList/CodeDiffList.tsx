import React from 'react'
import { IconButton, Tooltip, Typography, useTheme } from '@material-ui/core'
import { AutoSizer, CellMeasurer, CellMeasurerCache, List, WindowScroller } from 'react-virtualized'
import { connect, ConnectedProps } from 'react-redux'
import { AppState } from 'store'
import { ReactDiffViewerProps } from './ReactDiffViewer'
import SplitUnifiedViewButton from './CodeDiff/SplitUnifiedViewButton'
import CodeDiff from './CodeDiff'
import { useStyles } from './styles'

export interface ToolBarButton {
  Icon: React.FunctionComponent<React.SVGProps<SVGSVGElement>>
  tip?: string
  onClick: () => void
}

interface CodeDiffListProps<ListItem>
  extends Omit<
      ReactDiffViewerProps,
      'splitView' | 'oldValue' | 'newValue' | 'showDiffOnly' | 'leftOffset' | 'rightOffset'
    >,
    PropsFromRedux {
  title: string
  toolbarButtons?: ToolBarButton[]
  list: ListItem[]
  getDiffTitle: (listItem: ListItem) => React.ReactNode
  getOldValue: (ListItem: ListItem) => string
  getNewValue: (listItem: ListItem) => string
  getLeftOffset?: (listItem: ListItem) => number
  getRightOffset?: (listItem: ListItem) => number
  defaultExpanded?: boolean
  language: string
}

const CodeDiffList = <ListItem extends object>({
  title,
  list,
  language,
  defaultExpanded,
  toolbarButtons,
  getDiffTitle,
  getOldValue,
  getNewValue,
  getLeftOffset,
  getRightOffset,
  settings,
  ...props
}: CodeDiffListProps<ListItem>) => {
  const classes = useStyles()
  const theme = useTheme()
  const cache = new CellMeasurerCache({ fixedWidth: true, defaultHeight: 1000 })

  return (
    <section>
      <div className={classes.titleField}>
        <Typography className={classes.title} gutterBottom>
          {title}
        </Typography>
        {toolbarButtons &&
          toolbarButtons.map(({ Icon, onClick, tip }, index) => {
            const iconButton = (
              <IconButton onClick={onClick}>
                <Icon />
              </IconButton>
            )
            return (
              <div key={index}>
                {(tip && (
                  <Tooltip title={tip} placement="top">
                    {iconButton}
                  </Tooltip>
                )) || <>{iconButton}</>}
              </div>
            )
          })}
        <SplitUnifiedViewButton />
      </div>
      <AutoSizer disableHeight>
        {({ width }) => (
          <WindowScroller>
            {({ height, scrollTop }) => (
              <List
                className={classes.codeList}
                scrollTop={scrollTop}
                autoHeight
                rowCount={list.length}
                rowHeight={params => cache.rowHeight(params) + 60}
                deferredMeasurementCache={cache}
                rowRenderer={({ index, parent, key, style }) => (
                  <CellMeasurer
                    key={key}
                    cache={cache}
                    parent={parent}
                    columnIndex={0}
                    rowIndex={index}
                  >
                    <div style={style}>
                      <CodeDiff
                        title={getDiffTitle(list[index])}
                        splitView={settings.splitCodeView === 'split'}
                        oldValue={getOldValue(list[index])}
                        newValue={getNewValue(list[index])}
                        leftOffset={
                          getLeftOffset !== undefined ? getLeftOffset(list[index]) : undefined
                        }
                        rightOffset={
                          getRightOffset !== undefined ? getRightOffset(list[index]) : undefined
                        }
                        language="java"
                        useDarkTheme={theme.palette.type === 'dark'}
                        defaultExpanded
                        showDiffOnly={false}
                        {...props}
                      />
                    </div>
                  </CellMeasurer>
                )}
                width={width}
                height={height}
              />
            )}
          </WindowScroller>
        )}
      </AutoSizer>
    </section>
  )
}
const mapStateToProps = (state: AppState) => ({
  settings: state.settings.settings
})

const mapDispatchToProps = () => ({})
const connector = connect(mapStateToProps, mapDispatchToProps)

type PropsFromRedux = ConnectedProps<typeof connector>

export default connector(CodeDiffList)
