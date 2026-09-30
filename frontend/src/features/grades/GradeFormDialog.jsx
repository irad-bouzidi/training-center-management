import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
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
import { Textarea } from '@/components/ui/textarea'
import { ASSESSMENT_TYPES } from './gradeDisplay'
import { useCreateGradeMutation, useUpdateGradeMutation } from './hooks'
import { apiErrorMessage } from '@/api/serverErrors'

/**
 * Mirrors backend/src/main/java/com/tcm/grade/dto/GradeRequest.java.
 * `score <= maxScore` is checked here as well as server-side so the trainer
 * finds out before a round trip; the enrollment and ownership rules can only
 * be checked by the server, and come back inline below. Messages are i18n
 * keys in the `grades` namespace, translated where they render.
 */
const schema = z
  .object({
    assessmentType: z.enum(ASSESSMENT_TYPES),
    title: z.string().min(1, 'form.errors.titleRequired').max(200, 'form.errors.titleTooLong'),
    score: z.coerce.number({ message: 'form.errors.scoreRequired' }).min(0, 'form.errors.scoreNegative'),
    maxScore: z.coerce.number({ message: 'form.errors.maxScoreRequired' }).positive('form.errors.maxScorePositive'),
    weight: z.coerce.number({ message: 'form.errors.weightRequired' }).positive('form.errors.weightPositive'),
    comments: z.string().optional(),
  })
  .refine((values) => values.score <= values.maxScore, {
    path: ['score'],
    message: 'form.errors.scoreExceedsMax',
  })

/**
 * Records or amends one assessment - see
 * docs/tasks/TCM-24-frontend-grades.md step 2. `grade` is null when adding;
 * an existing entry when editing, in which case the student and course are
 * fixed (the backend keeps a grade attached to the pair it was recorded
 * for).
 *
 * Mounted only while in use (see GradebookPage), so the form starts from its
 * subject every time rather than being reset.
 */
export function GradeFormDialog({ onOpenChange, courseId, student, grade }) {
  const { t } = useTranslation('grades')
  const isEdit = Boolean(grade)
  const [serverError, setServerError] = useState(null)
  const createGrade = useCreateGradeMutation()
  const updateGrade = useUpdateGradeMutation()
  const mutation = isEdit ? updateGrade : createGrade

  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(schema),
    defaultValues: isEdit
      ? {
          assessmentType: grade.assessmentType,
          title: grade.title,
          score: grade.score,
          maxScore: grade.maxScore,
          weight: grade.weight,
          comments: grade.comments ?? '',
        }
      : { assessmentType: 'EXAM', title: '', score: '', maxScore: '100', weight: '', comments: '' },
  })

  function onSubmit(values) {
    setServerError(null)
    const payload = {
      studentId: student.studentId,
      courseId,
      assessmentType: values.assessmentType,
      title: values.title,
      score: values.score,
      maxScore: values.maxScore,
      weight: values.weight,
      comments: values.comments || null,
    }

    mutation.mutate(isEdit ? { id: grade.id, ...payload } : payload, {
      onSuccess: () => onOpenChange(false),
      // The error is kept and translated at render, so it follows a language switch.
      onError: setServerError,
    })
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? t('form.editTitle') : t('form.addTitle')}</DialogTitle>
            <DialogDescription>{student.studentName}</DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="assessmentType">{t('form.type')}</Label>
              <Controller
                control={control}
                name="assessmentType"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="assessmentType">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ASSESSMENT_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {t(`common:enums.assessmentType.${type}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="title">{t('form.title')}</Label>
              <Input id="title" maxLength={200} placeholder={t('form.titlePlaceholder')} {...register('title')} />
              {errors.title && <p className="text-sm text-destructive">{t(errors.title.message)}</p>}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="score">{t('form.score')}</Label>
              <Input id="score" type="number" step="0.01" min="0" {...register('score')} />
              {errors.score && <p className="text-sm text-destructive">{t(errors.score.message)}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="maxScore">{t('form.maxScore')}</Label>
              <Input id="maxScore" type="number" step="0.01" min="0.01" {...register('maxScore')} />
              {errors.maxScore && <p className="text-sm text-destructive">{t(errors.maxScore.message)}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="weight">{t('form.weight')}</Label>
              <Input id="weight" type="number" step="0.01" min="0.01" {...register('weight')} />
              {errors.weight && <p className="text-sm text-destructive">{t(errors.weight.message)}</p>}
            </div>
          </div>

          <p className="text-xs text-muted-foreground">
            {t('form.weightHint')}
          </p>

          <div className="space-y-2">
            <Label htmlFor="comments">{t('form.comments')}</Label>
            <Textarea id="comments" rows={3} {...register('comments')} />
          </div>

          {serverError && <p className="text-sm text-destructive">{apiErrorMessage(serverError, t('form.saveFailed'))}</p>}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              {t('common:actions.cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {mutation.isPending ? t('common:actions.saving') : isEdit ? t('form.saveChanges') : t('form.addTitle')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
