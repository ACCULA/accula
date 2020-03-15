import React from 'react'
import { BrowserRouter, Route, Switch } from 'react-router-dom'

import { Container } from 'react-bootstrap'
import { Navbar } from './components/Navbar'
import { About } from './pages/About'
import { Home } from './pages/Home'

const App: React.FC = () => (
  <BrowserRouter>
    <Navbar />
    <Switch>
      <Container>
        <Route path="/" component={Home} exact />
        <Route path="/about" component={About} />
      </Container>
    </Switch>
  </BrowserRouter>
)

export default App
