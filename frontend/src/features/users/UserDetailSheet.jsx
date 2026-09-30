import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { useUserQuery } from './hooks'
import { formatDate, fullName } from './userDisplay'

function Field({ label, value }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value}</p>
    </div>
  )
}

/** Read-only user detail, opened from a row's "View" action. */
export function UserDetailSheet({ open, onOpenChange, userId }) {
  const { t } = useTranslation('users')
  const { data: user, isLoading } = useUserQuery(userId)

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{user ? fullName(user) : t('detail.title')}</SheetTitle>
          <SheetDescription>{t('detail.description')}</SheetDescription>
        </SheetHeader>

        <div className="space-y-4 px-4">
          {isLoading && <p className="text-sm text-muted-foreground">{t('common:states.loading')}</p>}

          {user && (
            <>
              <div className="flex gap-2">
                <Badge variant="outline">{t(`common:enums.role.${user.role}`)}</Badge>
                <Badge variant={user.status === 'ACTIVE' ? 'secondary' : 'destructive'}>
                  {t(`common:enums.accountStatus.${user.status}`)}
                </Badge>
              </div>

              <Field label={t('fields.email')} value={user.email} />
              <Field label={t('fields.phone')} value={user.phone || '—'} />
              <Field label={t('fields.created')} value={formatDate(user.createdAt)} />
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
