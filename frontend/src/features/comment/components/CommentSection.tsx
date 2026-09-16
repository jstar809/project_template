import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { commentApi, commentKeys, type Comment } from '../api'
import { postKeys } from '@/features/post/api'
import type { ApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/lib/format'
import { useAuthStore } from '@/shared/store/authStore'
import { EmptyState, ErrorState } from '@/shared/ui/States'

export function CommentSection({ postId }: { postId: number }) {
  const queryClient = useQueryClient()
  const isLoggedIn = useAuthStore((state) => state.status === 'authenticated')

  const comments = useQuery({
    queryKey: commentKeys.list(postId),
    queryFn: () => commentApi.list(postId),
  })

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: commentKeys.list(postId) })
    await queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) })
  }

  const create = useMutation({
    mutationFn: (payload: { content: string; parentId?: number }) =>
      commentApi.create(postId, payload),
    onSuccess: refresh,
    onError: (error: ApiError) => toast.error(error.message),
  })

  const update = useMutation({
    mutationFn: ({ id, content }: { id: number; content: string }) =>
      commentApi.update(id, content),
    onSuccess: refresh,
    onError: (error: ApiError) => toast.error(error.message),
  })

  const remove = useMutation({
    mutationFn: (id: number) => commentApi.remove(id),
    onSuccess: refresh,
    onError: (error: ApiError) => toast.error(error.message),
  })

  return (
    <section className="space-y-4">
      <h2 className="text-sm font-medium">댓글 {comments.data?.length ?? 0}</h2>

      {isLoggedIn ? (
        <CommentForm
          submitLabel="댓글 등록"
          pending={create.isPending}
          onSubmit={(content) => create.mutate({ content })}
        />
      ) : (
        <p className="text-sm text-muted-foreground">
          댓글을 쓰려면{' '}
          <Link to="/login" className="underline underline-offset-4">
            로그인
          </Link>
          이 필요합니다.
        </p>
      )}

      {comments.isLoading && <Skeleton className="h-24 w-full" />}
      {comments.isError && (
        <ErrorState error={comments.error} onRetry={() => void comments.refetch()} />
      )}
      {comments.data?.length === 0 && <EmptyState message="첫 댓글을 남겨보세요." />}

      <ul className="space-y-3">
        {comments.data?.map((comment) => (
          <li key={comment.id}>
            <CommentItem
              comment={comment}
              canReply={isLoggedIn}
              onReply={(content) => create.mutate({ content, parentId: comment.id })}
              onUpdate={(content) => update.mutate({ id: comment.id, content })}
              onDelete={() => remove.mutate(comment.id)}
            />

            {comment.replies.length > 0 && (
              <ul className="mt-2 space-y-2 border-l pl-4">
                {comment.replies.map((reply) => (
                  <li key={reply.id}>
                    <CommentItem
                      comment={reply}
                      canReply={false}
                      onUpdate={(content) => update.mutate({ id: reply.id, content })}
                      onDelete={() => remove.mutate(reply.id)}
                    />
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

function CommentItem({
  comment,
  canReply,
  onReply,
  onUpdate,
  onDelete,
}: {
  comment: Comment
  canReply: boolean
  onReply?: (content: string) => void
  onUpdate: (content: string) => void
  onDelete: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [replying, setReplying] = useState(false)

  const masked = comment.deleted || comment.hidden

  return (
    <div className="rounded-md border p-3">
      <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
        <span className="font-medium text-foreground">
          {comment.authorNickname ?? '알 수 없음'}
        </span>
        <span>{formatDateTime(comment.createdAt)}</span>
      </div>

      {editing ? (
        <div className="mt-2">
          <CommentForm
            initialValue={comment.content}
            submitLabel="수정"
            onCancel={() => setEditing(false)}
            onSubmit={(content) => {
              onUpdate(content)
              setEditing(false)
            }}
          />
        </div>
      ) : (
        <p
          className={`mt-1 text-sm whitespace-pre-wrap ${masked ? 'text-muted-foreground italic' : ''}`}
        >
          {comment.content}
        </p>
      )}

      {!editing && (
        <div className="mt-2 flex gap-1">
          {canReply && onReply && (
            <Button variant="ghost" size="sm" onClick={() => setReplying((prev) => !prev)}>
              답글
            </Button>
          )}
          {comment.canEdit && (
            <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
              수정
            </Button>
          )}
          {comment.canDelete && (
            <Button variant="ghost" size="sm" onClick={onDelete}>
              삭제
            </Button>
          )}
        </div>
      )}

      {replying && onReply && (
        <div className="mt-2">
          <CommentForm
            submitLabel="답글 등록"
            placeholder="답글을 입력하세요."
            onCancel={() => setReplying(false)}
            onSubmit={(content) => {
              onReply(content)
              setReplying(false)
            }}
          />
        </div>
      )}
    </div>
  )
}

function CommentForm({
  initialValue = '',
  submitLabel,
  placeholder = '댓글을 입력하세요.',
  pending = false,
  onSubmit,
  onCancel,
}: {
  initialValue?: string
  submitLabel: string
  placeholder?: string
  pending?: boolean
  onSubmit: (content: string) => void
  onCancel?: () => void
}) {
  const [value, setValue] = useState(initialValue)

  return (
    <form
      className="space-y-2"
      onSubmit={(event) => {
        event.preventDefault()
        const trimmed = value.trim()
        if (!trimmed) {
          toast.error('내용을 입력해 주세요.')
          return
        }
        onSubmit(trimmed)
        setValue('')
      }}
    >
      <Textarea
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder={placeholder}
        maxLength={1000}
        rows={3}
        aria-label={submitLabel}
      />
      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button type="button" variant="ghost" size="sm" onClick={onCancel}>
            취소
          </Button>
        )}
        <Button type="submit" size="sm" disabled={pending}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
