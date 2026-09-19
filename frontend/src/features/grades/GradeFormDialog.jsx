import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
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
import { ASSESSMENT_TYPES, titleCase } from './gradeDisplay'
import { errorMessage, useCreateGradeMutation, useUpdateGradeMutation } from './hooks'

/**
 * Mirrors backend/src/main/java/com/tcm/grade/dto/GradeRequest.java.
 * `score <= maxScore` is checked here as well as server-side so the trainer
 * finds out before a round trip; the enrollment and ownership rules can only
 * be checked by the server, and come back inline below.
 */
const schema = z
  .object({
    assessmentType: z.enum(ASSESSMENT_TYPES),
    title: z.string().min(1, 'Title is required').max(200),
    score: z.coerce.number({ message: 'Score is required' }).min(0, 'Score must not be negative'),
    maxScore: z.coerce.number({ message: 'Maximum is required' }).positive('Maximum must be greater than zero'),
    weight: z.coerce.number({ message: 'Weight is required' }).positive('Weight must be greater than zero'),
    comments: z.string().optional(),
  })
  .refine((values) => values.score <= values.maxScore, {
    path: ['score'],
    message: 'Score must not exceed the maximum',
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
      onError: (error) => setServerError(errorMessage(error, 'Failed to save assessment')),
    })
  }

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? 'Edit assessment' : 'Add assessment'}</DialogTitle>
            <DialogDescription>{student.studentName}</DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="assessmentType">Type</Label>
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
                          {titleCase(type)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="title">Title</Label>
              <Input id="title" placeholder="Midterm exam" {...register('title')} />
              {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="score">Score</Label>
              <Input id="score" type="number" step="0.01" min="0" {...register('score')} />
              {errors.score && <p className="text-sm text-destructive">{errors.score.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="maxScore">Out of</Label>
              <Input id="maxScore" type="number" step="0.01" min="0.01" {...register('maxScore')} />
              {errors.maxScore && <p className="text-sm text-destructive">{errors.maxScore.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="weight">Weight</Label>
              <Input id="weight" type="number" step="0.01" min="0.01" {...register('weight')} />
              {errors.weight && <p className="text-sm text-destructive">{errors.weight.message}</p>}
            </div>
          </div>

          <p className="text-xs text-muted-foreground">
            Weights are each assessment’s share of the final mark. They needn’t add up to 100 — the average divides
            by the weights recorded so far, so a course can be graded before every assessment is set.
          </p>

          <div className="space-y-2">
            <Label htmlFor="comments">Comments</Label>
            <Textarea id="comments" rows={3} {...register('comments')} />
          </div>

          {serverError && <p className="text-sm text-destructive">{serverError}</p>}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEdit ? 'Save changes' : 'Add assessment'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
