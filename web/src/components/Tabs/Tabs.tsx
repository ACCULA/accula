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
  onChange?: (tab: Tab) => void
}

const Tabs = ({ tabs, onChange }: TabsProps) => {
  const classes = useStyles()
  const [activeTab, setActiveTab] = useState(0)

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
        <div>
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
