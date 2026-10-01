import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useCoursesQuery, useTrainersQuery } from '@/features/courses/hooks'
import { toTimeInputValue } from './scheduleDisplay'
import { useCreateSessionMutation, useUpdateSessionMutation } from './hooks'
import { apiErrorMessage } from '@/api/serverErrors'

// The course picker lists everything an admin could schedule - one large page
// rather than a paged combobox, same call the trainer picker makes.
const COURSE_PICKER_SIZE = 200

/**
 * Mirrors backend/src/main/java/com/tcm/schedule/dto/ClassSessionRequest.java.
 * `endTime > startTime` is checked here as well as server-side so the admin
 * finds out before a round trip; the overlap rule can only be checked by the
 * server, and comes back as the 409 shown inline below.
 *
 * Date and time use the native inputs rather than a Calendar popover: their
 * values are already the `yyyy-MM-dd` / `HH:mm` strings the API wants, and
 * the task leaves the calendar grid explicitly optional (step 3).
 *
 * Messages are i18n keys in the `schedule` namespace, translated where they
 * render.
 */
const schema = z
  .object({
    courseId: z.string().min(1, 'form.errors.courseRequired'),
    trainerId: z.string().min(1, 'form.errors.trainerRequired'),
    classroom: z.string().min(1, 'form.errors.classroomRequired'),
    sessionDate: z.string().min(1, 'form.errors.dateRequired'),
    startTime: z.string().min(1, 'form.errors.startTimeRequired'),
    endTime: z.string().min(1, 'form.errors.endTimeRequired'),
  })
  .refine((values) => values.endTime > values.startTime, {
    path: ['endTime'],
    message: 'form.errors.endAfterStart',
  })

const EMPTY_VALUES = {
  courseId: '',
  trainerId: '',
  classroom: '',
  sessionDate: '',
  startTime: '',
  endTime: '',
}

/**
 * Create/edit dialog for a class session (ADMIN) - see
 * docs/tasks/TCM-18-frontend-scheduling.md step 2. `session` is null for
 * create; an existing session for edit.
 *
 * `defaultCourseId` pre-selects a course when the dialog is opened from that
 * course's own Schedule tab.
 */
export function ScheduleFormDialog({ open, onOpenChange, session, defaultCourseId }) {
  const { t } = useTranslation('schedule')
  const isEdit = Boolean(session)
  const [serverError, setServerError] = useState(null)

  const { data: coursesPage } = useCoursesQuery({ size: COURSE_PICKER_SIZE })
  const { data: trainers } = useTrainersQuery()
  const createSession = useCreateSessionMutation()
  const updateSession = useUpdateSessionMutation()

  const courses = coursesPage?.content ?? []

  const {
    register,
    control,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(schema),
    values: isEdit
      ? {
          courseId: session.course.id,
          trainerId: session.trainer.id,
          classroom: session.classroom,
          sessionDate: session.sessionDate,
          startTime: toTimeInputValue(session.startTime),
          endTime: toTimeInputValue(session.endTime),
        }
      : { ...EMPTY_VALUES, courseId: defaultCourseId ?? '' },
  })

  // useWatch rather than the form's own watch(): watch() re-renders on every
  // keystroke in any field and returns values the React Compiler can't
  // memoize, where a subscription to these two is all this needs.
  const courseId = useWatch({ control, name: 'courseId' })
  const trainerId = useWatch({ control, name: 'trainerId' })

  // Picking a course pre-fills its primary trainer, who usually is the one
  // teaching it - only as a starting point, and never over a choice already
  // made (including the assignment an edited session came in with). Reading
  // through `coursesPage` keeps the dependency referentially stable; the
  // `?? []` the render uses would make a new array every time.
  useEffect(() => {
    if (!courseId || trainerId) return
    const primaryTrainerId = coursesPage?.content
      ?.find((course) => course.id === courseId)?.primaryTrainer?.id
    if (primaryTrainerId) setValue('trainerId', primaryTrainerId)
  }, [courseId, trainerId, coursesPage, setValue])

  async function onSubmit(values) {
    setServerError(null)
    try {
      if (isEdit) {
        await updateSession.mutateAsync({ id: session.id, ...values })
      } else {
        await createSession.mutateAsync(values)
      }
      onOpenChange(false)
    } catch (error) {
      // Chiefly the 409 a double-booked trainer or classroom produces: the
      // message names which one clashed, so it belongs in the dialog the
      // admin is about to correct, not in a toast.
      // The error itself is kept and translated at render, so it follows a
      // language switch.
      setServerError(error)
    }
  }

  function handleOpenChange(next) {
    if (!next) {
      reset(EMPTY_VALUES)
      setServerError(null)
    }
    onOpenChange(next)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEdit ? t('form.editTitle') : t('form.createTitle')}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? t('form.editDescription', { course: session.course.name })
              : t('form.createDescription')}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-2">
            <Label htmlFor="courseId">{t('form.course')}</Label>
            <Controller
              name="courseId"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="courseId" className="w-full" aria-invalid={Boolean(errors.courseId)}>
                    <SelectValue placeholder={t('form.coursePlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    {courses.map((course) => (
                      <SelectItem key={course.id} value={course.id}>
                        {course.name} ({course.code})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.courseId && <p className="text-sm text-destructive">{t(errors.courseId.message)}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="trainerId">{t('form.trainer')}</Label>
            <Controller
              name="trainerId"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="trainerId" className="w-full" aria-invalid={Boolean(errors.trainerId)}>
                    <SelectValue placeholder={t('form.trainerPlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    {trainers?.map((trainer) => (
                      <SelectItem key={trainer.id} value={trainer.id}>
                        {trainer.firstName} {trainer.lastName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.trainerId && <p className="text-sm text-destructive">{t(errors.trainerId.message)}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="classroom">{t('form.classroom')}</Label>
              <Input id="classroom" aria-invalid={Boolean(errors.classroom)} {...register('classroom')} />
              {errors.classroom && <p className="text-sm text-destructive">{t(errors.classroom.message)}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="sessionDate">{t('form.date')}</Label>
              <Input
                id="sessionDate"
                type="date"
                aria-invalid={Boolean(errors.sessionDate)}
                {...register('sessionDate')}
              />
              {errors.sessionDate && <p className="text-sm text-destructive">{t(errors.sessionDate.message)}</p>}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="startTime">{t('form.startTime')}</Label>
              <Input id="startTime" type="time" aria-invalid={Boolean(errors.startTime)} {...register('startTime')} />
              {errors.startTime && <p className="text-sm text-destructive">{t(errors.startTime.message)}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="endTime">{t('form.endTime')}</Label>
              <Input id="endTime" type="time" aria-invalid={Boolean(errors.endTime)} {...register('endTime')} />
              {errors.endTime && <p className="text-sm text-destructive">{t(errors.endTime.message)}</p>}
            </div>
          </div>

          {serverError && (
            <p role="alert" className="rounded-md border border-destructive/50 p-3 text-sm text-destructive">
              {apiErrorMessage(serverError, t('form.saveFailed'))}
            </p>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              {t('common:actions.cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? t('common:actions.saving') : t('common:actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
