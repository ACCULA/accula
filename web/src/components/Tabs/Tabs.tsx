import React from 'react'
import { Tabs as MuiTabs, Tab } from '@material-ui/core'
import { useStyles } from './styles'

interface Tab {
  text: string
}

interface TabsProps {
  tabs?: Tab[]
  onChange?: (tab: Tab) => void
}

const Tabs = ({ tabs, onChange }: TabsProps) => {
  const classes = useStyles()
  const [activeTab, setActiveTab] = React.useState(0)

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

  return (
    <MuiTabs
      className={classes.tabs}
      value={tabs ? activeTab : false}
      onChange={handleChange}
      aria-label="tabs"
    >
      {tabs ? (
        <>
          {tabs.map(({ text }, index) => (
            <Tab key={text} label={`${text}`} {...a11yProps(index)} />
          ))}
        </>
      ) : (
        <Tab className={classes.dummyTab} />
      )}
    </MuiTabs>
  )
}

export default Tabs
