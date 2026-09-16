import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { CommentSection } from '@/features/comment/components/CommentSection'
import { formatFileSize } from '@/features/file/api'
import { postApi, postKeys } from '../api'
import type { ApiError } from '@/shared/api/errors'
import { formatDateTime } from '@/shared/lib/format'
import { RichTextViewer } from '@/shared/ui/RichTextViewer'
import { ErrorState } from '@/shared/ui/States'

export default function PostDetailPage() {
  const { slug = '', id = '' } = useParams()
  const postId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const post = useQuery({
    queryKey: postKeys.detail(postId),
    queryFn: () => postApi.detail(postId),
    enabled: Number.isFinite(postId),
  })

  const remove = useMutation({
    mutationFn: () => postApi.remove(postId),
    onSuccess: async () => {
      toast.success('글을 삭제했습니다.')
      await queryClient.invalidateQueries({ queryKey: postKeys.all })
      navigate(`/boards/${slug}`, { replace: true })
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  if (post.isLoading) return <Skeleton className="h-96 w-full" />
  if (post.isError) return <ErrorState error={post.error} />
  if (!post.data) return null

  const data = post.data

  return (
    <article className="space-y-4">
      <header className="space-y-2">
        <div className="flex items-center gap-2">
          {data.notice && <Badge>공지</Badge>}
          <h1 className="text-xl font-semibold break-words">{data.title}</h1>
        </div>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span>{data.author.nickname}</span>
          <span>{formatDateTime(data.createdAt)}</span>
          <span>조회 {data.viewCount}</span>
          <span>댓글 {data.commentCount}</span>
        </div>
      </header>

      <Separator />

      <RichTextViewer html={data.content} />

      {data.attachments.length > 0 && (
        <section className="rounded-md border p-3">
          <h2 className="mb-2 text-sm font-medium">첨부 파일</h2>
          <ul className="space-y-1">
            {data.attachments.map((file) => (
              <li key={file.id}>
                <a
                  href={file.url}
                  className="flex items-center gap-2 text-sm hover:underline"
                  download={file.originalName}
                >
                  <Download className="size-3.5" />
                  <span>{file.originalName}</span>
                  <span className="text-xs text-muted-foreground">{formatFileSize(file.size)}</span>
                </a>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="flex flex-wrap justify-between gap-2">
        <Button variant="outline" asChild>
          <Link to={`/boards/${slug}`}>목록</Link>
        </Button>

        {(data.canEdit || data.canDelete) && (
          <div className="flex gap-2">
            {data.canEdit && (
              <Button variant="outline" asChild>
                <Link to={`/boards/${slug}/posts/${postId}/edit`}>수정</Link>
              </Button>
            )}
            {data.canDelete && (
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button variant="destructive">삭제</Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>글을 삭제할까요?</AlertDialogTitle>
                    <AlertDialogDescription>
                      삭제한 글은 목록에서 사라집니다.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>취소</AlertDialogCancel>
                    <AlertDialogAction onClick={() => remove.mutate()}>삭제</AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            )}
          </div>
        )}
      </div>

      <Separator />

      <CommentSection postId={postId} />
    </article>
  )
}
