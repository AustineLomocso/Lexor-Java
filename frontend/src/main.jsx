import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import LexorIDE from './LexorIDE.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <LexorIDE />
  </StrictMode>
)
