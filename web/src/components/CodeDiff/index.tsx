// Fork of https://github.com/praneshr/react-diff-viewer
import React, { useState } from 'react'
import cn from 'classnames'

import { Panel } from 'react-bootstrap'
import { computeLineInformation } from './computeLines'
import {
  CodeDiffProps,
  DiffInformation,
  DiffMethod,
  DiffType,
  LineInformation,
  LineNumberPrefix
} from './types'

const CodeDiff = ({
  oldValue = '',
  newValue = '',
  splitView = true,
  highlightLines = [],
  disableWordDiff = false,
  compareMethod = DiffMethod.CHARS,
  hideLineNumbers = false,
  extraLinesSurroundingDiff = 3,
  showDiffOnly = true,
  leftOffset = 0,
  rightOffset = 0,
  leftTitle,
  rightTitle,
  onLineNumberClick,
  codeFoldMessageRenderer,
  renderContent
}: CodeDiffProps) => {
  if (typeof oldValue !== 'string' || typeof newValue !== 'string') {
    throw Error('"oldValue" and "newValue" should be strings')
  }

  const [expandedBlocks, setExpandedBlocks] = useState([])
  const [isShow, setShow] = useState(true)

  /**
   * Pushes the target expanded code block to the state. During the re-render,
   * value is used to expand/fold unmodified code.
   */
  const onBlockExpand = (id: number): void => {
    const prevState = expandedBlocks.slice()
    prevState.push(id)
    setExpandedBlocks(prevState)
  }

  /**
   * Returns a function with clicked line number in the closure. Returns an no-op function when no
   * onLineNumberClick handler is supplied.
   *
   * @param id Line id of a line.
   */
  const onLineNumberClickProxy = (id: string): any => {
    if (onLineNumberClick) {
      return (e: any): void => onLineNumberClick(id, e)
    }
    return (): void => {}
  }

  /**
   * Maps over the word diff and constructs the required React elements to show word diff.
   *
   * @param diffArray Word diff information derived from line information.
   * @param renderer Optional renderer to format diff words. Useful for syntax highlighting.
   */
  const renderWordDiff = (
    diffArray: DiffInformation[],
    renderer?: (chunk: string) => JSX.Element
  ): JSX.Element[] => {
    return diffArray.map(
      (wordDiff, i): JSX.Element => {
        return (
          <span
            key={i}
            className={cn('word-diff', {
              'word-added': wordDiff.type === DiffType.ADDED,
              'word-removed': wordDiff.type === DiffType.REMOVED
            })}
          >
            {renderer ? renderer(wordDiff.value as string) : wordDiff.value}
          </span>
        )
      }
    )
  }

  /**
   * Maps over the line diff and constructs the required react elements to show line diff. It calls
   * renderWordDiff when encountering word diff. takes care of both inline and split view line
   * renders.
   *
   * @param lineNumber Line number of the current line.
   * @param type Type of diff of the current line.
   * @param prefix Unique id to prefix with the line numbers.
   * @param value Content of the line. It can be a string or a word diff array.
   * @param additionalLineNumber Additional line number to be shown. Useful for rendering inline
   *  diff view. Right line number will be passed as additionalLineNumber.
   * @param additionalPrefix Similar to prefix but for additional line number.
   */
  const renderLine = (
    lineNumber: number,
    type: DiffType,
    prefix: LineNumberPrefix,
    value: string | DiffInformation[],
    additionalLineNumber?: number,
    additionalPrefix?: LineNumberPrefix
  ): JSX.Element => {
    const lineNumberTemplate = `${prefix}-${lineNumber}`
    const additionalLineNumberTemplate = `${additionalPrefix}-${additionalLineNumber}`
    const highlightLine =
      highlightLines.includes(lineNumberTemplate) ||
      highlightLines.includes(additionalLineNumberTemplate)
    const added = type === DiffType.ADDED
    const removed = type === DiffType.REMOVED
    let content
    if (Array.isArray(value)) {
      content = renderWordDiff(value, renderContent)
    } else if (renderContent) {
      content = renderContent(value)
    } else {
      content = value
    }

    return (
      <React.Fragment>
        {!hideLineNumbers && (
          <td
            onClick={lineNumber && onLineNumberClickProxy(lineNumberTemplate)}
            className={cn('gutter', {
              'empty-gutter': !lineNumber,
              'diff-added': added,
              'diff-removed': removed,
              'highlighted-gutter': highlightLine
            })}
          >
            <pre className="line-number">{lineNumber}</pre>
          </td>
        )}
        {!splitView && !hideLineNumbers && (
          <td
            onClick={additionalLineNumber && onLineNumberClickProxy(additionalLineNumberTemplate)}
            className={cn('gutter', {
              'empty-gtter': !additionalLineNumber,
              'diff-added': added,
              'diff-removed': removed,
              'highlighted-gutter': highlightLine
            })}
          >
            <pre className="line-number">{additionalLineNumber}</pre>
          </td>
        )}
        <td
          className={cn('marker', {
            'empty-line': !content,
            'diff-added': added,
            'diff-removed': removed,
            'highlighted-line': highlightLine
          })}
        >
          <pre>
            {added && '+'}
            {removed && '-'}
          </pre>
        </td>
        <td
          className={cn('content', {
            'empty-line': !content,
            'diff-added': added,
            'diff-removed': removed,
            'highlighted-line': highlightLine
          })}
        >
          <pre className="content-text">{content}</pre>
        </td>
      </React.Fragment>
    )
  }

  /**
   * Generates lines for split view.
   *
   * @param obj Line diff information.
   * @param obj.left Life diff information for the left pane of the split view.
   * @param obj.right Life diff information for the right pane of the split view.
   * @param index React key for the lines.
   */
  const renderSplitView = ({ left, right }: LineInformation, index: number): JSX.Element => {
    return (
      <tr key={index} className="line">
        {renderLine(left.lineNumber, left.type, LineNumberPrefix.LEFT, left.value)}
        {renderLine(right.lineNumber, right.type, LineNumberPrefix.RIGHT, right.value)}
      </tr>
    )
  }

  /**
   * Generates lines for inline view.
   *
   * @param obj Line diff information.
   * @param obj.left Life diff information for the added section of the inline view.
   * @param obj.right Life diff information for the removed section of the inline view.
   * @param index React key for the lines.
   */
  const renderInlineView = ({ left, right }: LineInformation, index: number): JSX.Element => {
    let content
    if (left.type === DiffType.REMOVED && right.type === DiffType.ADDED) {
      return (
        <React.Fragment key={index}>
          <tr className="line">
            {renderLine(left.lineNumber, left.type, LineNumberPrefix.LEFT, left.value, null)}
          </tr>
          <tr className="line">
            {renderLine(null, right.type, LineNumberPrefix.RIGHT, right.value, right.lineNumber)}
          </tr>
        </React.Fragment>
      )
    }
    if (left.type === DiffType.REMOVED) {
      content = renderLine(left.lineNumber, left.type, LineNumberPrefix.LEFT, left.value, null)
    }
    if (left.type === DiffType.DEFAULT) {
      content = renderLine(
        left.lineNumber,
        left.type,
        LineNumberPrefix.LEFT,
        left.value,
        right.lineNumber,
        LineNumberPrefix.RIGHT
      )
    }
    if (right.type === DiffType.ADDED) {
      content = renderLine(null, right.type, LineNumberPrefix.RIGHT, right.value, right.lineNumber)
    }

    return (
      <tr key={index} className="line">
        {content}
      </tr>
    )
  }

  /**
   * Returns a function with clicked block number in the closure.
   *
   * @param id Cold fold block id.
   */
  const onBlockClickProxy = (id: number): any => (): void => onBlockExpand(id)

  /**
   * Generates cold fold block. It also uses the custom message renderer when available to show
   * cold fold messages.
   *
   * @param num Number of skipped lines between two blocks.
   * @param blockNumber Code fold block id.
   * @param leftBlockLineNumber First left line number after the current code fold block.
   * @param rightBlockLineNumber First right line number after the current code fold block.
   */
  const renderSkippedLineIndicator = (
    num: number,
    blockNumber: number,
    leftBlockLineNumber: number,
    rightBlockLineNumber: number
  ): JSX.Element => {
    const message = codeFoldMessageRenderer ? (
      codeFoldMessageRenderer(num, leftBlockLineNumber, rightBlockLineNumber)
    ) : (
      <pre className="code-fold-content">Expand {num} lines ...</pre>
    )
    const content = (
      <td>
        <div
          className="pointer"
          onClick={onBlockClickProxy(blockNumber)}
          onKeyDown={onBlockClickProxy(blockNumber)}
          tabIndex={0}
          role="button"
        >
          {message}
        </div>
      </td>
    )
    const isUnifiedViewWithoutLineNumbers = !splitView && !hideLineNumbers
    return (
      <tr key={`${leftBlockLineNumber}-${rightBlockLineNumber}`} className="code-fold">
        {!hideLineNumbers && <td className="code-fold-gutter" />}
        <td className={cn({ 'code-fold-gutter': isUnifiedViewWithoutLineNumbers })} />

        {/* Swap columns only for unified view without line numbers */}
        {isUnifiedViewWithoutLineNumbers ? (
          <React.Fragment>
            <td />
            {content}
          </React.Fragment>
        ) : (
          <React.Fragment>
            {content}
            <td />
          </React.Fragment>
        )}

        <td />
        <td />
      </tr>
    )
  }

  /**
   * Generates the entire diff view.
   */
  const renderDiff = (): JSX.Element[] => {
    const { lineInformation, diffLines } = computeLineInformation(
      oldValue,
      newValue,
      disableWordDiff,
      compareMethod,
      leftOffset,
      rightOffset
    )
    const extraLines = extraLinesSurroundingDiff < 0 ? 0 : extraLinesSurroundingDiff
    let skippedLines: number[] = []
    return lineInformation.map(
      (line: LineInformation, i: number): JSX.Element => {
        const diffBlockStart = diffLines[0]
        const currentPosition = diffBlockStart - i
        if (showDiffOnly) {
          if (currentPosition === -extraLines) {
            skippedLines = []
            diffLines.shift()
          }
          if (
            line.left.type === DiffType.DEFAULT &&
            (currentPosition > extraLines || typeof diffBlockStart === 'undefined') &&
            !expandedBlocks.includes(diffBlockStart)
          ) {
            skippedLines.push(i + 1)
            if (i === lineInformation.length - 1 && skippedLines.length > 1) {
              return renderSkippedLineIndicator(
                skippedLines.length,
                diffBlockStart,
                line.left.lineNumber,
                line.right.lineNumber
              )
            }
            return null
          }
        }

        const diffNodes = splitView ? renderSplitView(line, i) : renderInlineView(line, i)

        if (currentPosition === extraLines && skippedLines.length > 0) {
          const { length } = skippedLines
          skippedLines = []
          return (
            <React.Fragment key={i}>
              {renderSkippedLineIndicator(
                length,
                diffBlockStart,
                line.left.lineNumber,
                line.right.lineNumber
              )}
              {diffNodes}
            </React.Fragment>
          )
        }
        return diffNodes
      }
    )
  }

  const nodes = renderDiff()
  return (
    <Panel expanded={isShow} onToggle={show => setShow(show)} className="code-diff">
      <Panel.Heading>
        <i
          className={`fas ${isShow ? 'fa-chevron-down' : 'fa-chevron-right'} pointer`}
          onClick={() => setShow(!isShow)}
          style={{ marginRight: 10, width: 14, height: 14 }}
          role="button"
        />
        {leftTitle}
      </Panel.Heading>
      <Panel.Collapse>
        <table className={cn('diff-container', { 'split-view': splitView })}>
          <tbody>{nodes}</tbody>
        </table>
      </Panel.Collapse>
    </Panel>
  )
}

export { CodeDiff, DiffMethod }
