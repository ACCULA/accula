import React, { useState } from 'react'
import { Tabs as MuiTabs, Tab, Badge } from '@material-ui/core'
import { useStyles } from './styles'

export interface Tab {
  id: string
  text: string
  Icon?: React.FunctionComponent<React.SVGProps<SVGSVGElement>>
  badgeValue?: React.ReactNode
  badgeTip?: string
}

interface TabsProps {
  tabs?: Tab[]
  activeId?: string
  onChange?: (tab: Tab) => void
}

const Tabs = ({ tabs, activeId, onChange }: TabsProps) => {
  const classes = useStyles()
  const tab = tabs && activeId ? tabs.findIndex(t => t.id === activeId) : 0
  const [activeTab, setActiveTab] = useState(tab === -1 ? 0 : tab)

  const handleChange = (event: React.ChangeEvent<{}>, newValue: number) => {
    setActiveTab(newValue)
    if (onChange) {
      onChange(tabs[newValue])
    }
  }

  const a11yProps = (index: number) => {
    return {
      id: `simple-tab-${index}`,
      'aria-controls': `simple-tabpanel-${index}`
    }
  }

  const tabItems = tabs ? (
    tabs.map(({ text, Icon, badgeValue }, index) => {
      const label = (
        <div className={classes.tabContent}>
          {Icon && <Icon className={classes.tabImg} />}
          {badgeValue ? (
            <Badge className={classes.badge} badgeContent={badgeValue} max={99} color="secondary">
              <span>{text}</span>
            </Badge>
          ) : (
            <span>{text}</span>
          )}
        </div>
      )
      return <Tab className={classes.tab} key={text} label={label} {...a11yProps(index)} />
    })
  ) : (
    <Tab className={classes.dummyTab} />
  )

  return (
    <MuiTabs
      className={classes.tabs}
      value={tabs ? activeTab : false}
      onChange={handleChange}
      variant="scrollable"
      aria-label="tabs"
    >
      {tabItems}
    </MuiTabs>
  )
}

export default Tabs
