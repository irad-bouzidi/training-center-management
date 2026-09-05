import { zodResolver } from '@hookform/resolvers/zod'
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
import { useCreateCourseMutation, useTrainersQuery, useUpdateCourseMutation } from './hooks'

const NO_TRAINER = 'NONE'

// Mirrors backend/src/main/java/com/tcm/course/dto/CourseRequest.java, minus
// status - a new course always starts as DRAFT and an edit preserves the
// course's current status; Publish/Archive/Republish are separate actions
// (see CourseRowActions), same stub-shadcn-Form situation as LoginPage (see
// TCM-9) - react-hook-form is composed directly against Label/Input/Select.
const schema = z.object({
  code: z.string().min(1, 'Code is required'),
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  durationHours: z.coerce.number('Duration is required').int('Must be a whole number').positive('Must be positive'),
  capacity: z.coerce.number('Capacity is required').int('Must be a whole number').positive('Must be positive'),
  category: z.string().optional(),
  primaryTrainerId: z.string().optional(),
  price: z.coerce.number('Price is required').nonnegative('Must be zero or more'),
})

const EMPTY_VALUES = {
  code: '',
  name: '',
  description: '',
  durationHours: '',
  capacity: '',
  category: '',
  primaryTrainerId: NO_TRAINER,
  price: '',
}

/**
 * Create/edit dialog. `course` is null for create; an existing course for
 * edit - see docs/tasks/TCM-12-frontend-course-management.md step 3.
 */
export function CourseFormDialog({ open, onOpenChange, course }) {
  const isEdit = Boolean(course)
  const { data: trainers } = useTrainersQuery()
  const createCourse = useCreateCourseMutation()
  const updateCourse = useUpdateCourseMutation()

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(schema),
    values: isEdit
      ? {
          code: course.code,
          name: course.name,
          description: course.description ?? '',
          durationHours: course.durationHours,
          capacity: course.capacity,
          category: course.category ?? '',
          primaryTrainerId: course.primaryTrainer?.id ?? NO_TRAINER,
          price: course.price,
        }
      : EMPTY_VALUES,
  })

  async function onSubmit(values) {
    const payload = {
      ...values,
      primaryTrainerId: values.primaryTrainerId === NO_TRAINER ? null : values.primaryTrainerId,
      status: isEdit ? course.status : 'DRAFT',
    }
    try {
      if (isEdit) {
        await updateCourse.mutateAsync({ id: course.id, ...payload })
      } else {
        await createCourse.mutateAsync(payload)
      }
      onOpenChange(false)
    } catch {
      // Already surfaced via the mutation's onError toast (see hooks.js) -
      // keep the dialog open so the user can fix the input and retry.
    }
  }

  function handleOpenChange(next) {
    if (!next) reset(EMPTY_VALUES)
    onOpenChange(next)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit course' : 'New course'}</DialogTitle>
          <DialogDescription>
            {isEdit ? `Update ${course.name}.` : "Create a new course, then publish it when it's ready."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="code">Code</Label>
              <Input id="code" aria-invalid={Boolean(errors.code)} {...register('code')} />
              {errors.code && <p className="text-sm text-destructive">{errors.code.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" aria-invalid={Boolean(errors.name)} {...register('name')} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea id="description" {...register('description')} />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="durationHours">Duration (hours)</Label>
              <Input
                id="durationHours"
                type="number"
                min="1"
                aria-invalid={Boolean(errors.durationHours)}
                {...register('durationHours')}
              />
              {errors.durationHours && <p className="text-sm text-destructive">{errors.durationHours.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="capacity">Capacity</Label>
              <Input
                id="capacity"
                type="number"
                min="1"
                aria-invalid={Boolean(errors.capacity)}
                {...register('capacity')}
              />
              {errors.capacity && <p className="text-sm text-destructive">{errors.capacity.message}</p>}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Input id="category" {...register('category')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="price">Price</Label>
              <Input
                id="price"
                type="number"
                min="0"
                step="0.01"
                aria-invalid={Boolean(errors.price)}
                {...register('price')}
              />
              {errors.price && <p className="text-sm text-destructive">{errors.price.message}</p>}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="primaryTrainerId">Trainer</Label>
            <Controller
              name="primaryTrainerId"
              control={control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="primaryTrainerId" className="w-full">
                    <SelectValue placeholder="Select a trainer" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NO_TRAINER}>No trainer assigned</SelectItem>
                    {trainers?.map((trainer) => (
                      <SelectItem key={trainer.id} value={trainer.id}>
                        {trainer.firstName} {trainer.lastName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : 'Save'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
