import { Provider } from 'react-redux'
import { store } from 'store'
import React from 'react'
import ReactDOM from 'react-dom'
import { BrowserRouter, Route, Switch } from 'react-router-dom'
import 'styles/styles.scss'
import App from 'App'

ReactDOM.render(
  <Provider store={store}>
    <BrowserRouter>
      <Switch>
        <Route path="/" render={props => <App {...props} />} />
      </Switch>
    </BrowserRouter>
  </Provider>,
  document.getElementById('root')
)
