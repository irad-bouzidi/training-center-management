import { LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { LanguageSwitcher } from '@/components/LanguageSwitcher'
import { ThemeSwitcher } from '@/components/ThemeSwitcher'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuth } from '@/context/AuthContext'

function initials(name) {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

/**
 * Shared shell every role layout (AdminLayout/TrainerLayout/StudentLayout)
 * renders: sidebar nav + top bar with a user menu/logout.
 *
 * `titleKey` / `navItems[].labelKey` are `layout:` i18n keys (TCM-32).
 *
 * `navItems`: [{ labelKey, to?, enabled }] - items for features later tasks
 * build (enabled: false) render greyed out and unlinked instead of routing
 * anywhere, per docs/tasks/TCM-9-frontend-auth.md.
 */
export function AppShell({ titleKey, navItems }) {
  const { t } = useTranslation('layout')
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-svh">
      <aside className="w-56 shrink-0 border-r bg-sidebar p-4">
        <p className="mb-4 px-2 text-sm font-semibold text-sidebar-foreground">{t(titleKey)}</p>
        <nav className="space-y-1">
          {navItems.map((item) =>
            item.enabled ? (
              <Link
                key={item.to}
                to={item.to}
                className="block rounded-md px-2 py-1.5 text-sm text-sidebar-foreground hover:bg-sidebar-accent"
              >
                {t(item.labelKey)}
              </Link>
            ) : (
              <span
                key={item.labelKey}
                title={t('comingSoon')}
                className="block cursor-not-allowed rounded-md px-2 py-1.5 text-sm text-muted-foreground"
              >
                {t(item.labelKey)}
              </span>
            ),
          )}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-end gap-2 border-b px-6 py-3">
          <ThemeSwitcher />
          <LanguageSwitcher />
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="flex items-center gap-2 px-2">
                <Avatar className="size-8">
                  <AvatarFallback>{initials(user.name)}</AvatarFallback>
                </Avatar>
                <span className="text-sm">{user.name}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>{user.email}</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={handleLogout}>
                <LogOut />
                {t('logOut')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>

        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
