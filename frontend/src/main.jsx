import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { KeycloakProvider } from './auth/KeycloakContext.jsx'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <KeycloakProvider>
      <App />
    </KeycloakProvider>
  </React.StrictMode>,
)
