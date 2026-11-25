import { AppShell } from './components/AppShell'
import { useHashRoute } from './router/hashRouter'
import { DynamicScreen } from './screens/DynamicScreen'
import { screenRegistryById } from './screens/registry'
import { ThemeProvider } from './theme/ThemeProvider'

function CurrentScreen() {
  const { slug } = useHashRoute()

  if (!slug || slug === 'catalogo') return <DynamicScreen id="P01-login" />

  const entry = screenRegistryById.get(slug)
  if (!entry) return <DynamicScreen id="V03-pagina-nao-encontrada" />

  const Component = entry.Component
  return <Component />
}

function App() {
  return (
    <ThemeProvider>
      <AppShell>
        <CurrentScreen />
      </AppShell>
    </ThemeProvider>
  )
}

export default App
