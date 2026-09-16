import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Paperclip, X } from 'lucide-react'
import { useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { useBoard } from '@/features/board/hooks'
import { fileApi, formatFileSize, type UploadedFile } from '@/features/file/api'
import { postApi, postKeys, type PostDetail, type PostUpsertPayload } from '../api'
import { toApiError, type ApiError } from '@/shared/api/errors'
import { useAuthStore } from '@/shared/store/authStore'
import { RichTextEditor } from '@/shared/ui/RichTextEditor'
import { ErrorState } from '@/shared/ui/States'

/**
 * 작성과 수정을 같은 화면에서 처리한다. id가 있으면 수정 모드다.
 *
 * 데이터 로딩과 폼을 두 컴포넌트로 나눈다. 폼은 데이터가 준비된 뒤에만 렌더되므로
 * 초기값을 useState로 바로 넣을 수 있다(useEffect로 상태를 덮어쓰지 않는다).
 */
export default function PostEditorPage() {
  const { slug = '', id } = useParams()
  const postId = id ? Number(id) : null
  const isEdit = postId !== null

  const board = useBoard(slug)
  const existing = useQuery({
    queryKey: postKeys.detail(postId ?? 0),
    queryFn: () => postApi.detail(postId as number),
    enabled: isEdit,
  })

  if (board.isError) return <ErrorState error={board.error} />
  if (existing.isError) return <ErrorState error={existing.error} />
  if (board.isLoading || !board.data || (isEdit && !existing.data)) {
    return <Skeleton className="h-96 w-full" />
  }

  return (
    <PostForm
      slug={slug}
      postId={postId}
      attachmentEnabled={board.data.attachmentEnabled}
      initial={existing.data ?? null}
    />
  )
}

function PostForm({
  slug,
  postId,
  attachmentEnabled,
  initial,
}: {
  slug: string
  postId: number | null
  attachmentEnabled: boolean
  initial: PostDetail | null
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const isAdmin = useAuthStore((state) => state.user?.role === 'ADMIN')
  const isEdit = postId !== null

  const [title, setTitle] = useState(initial?.title ?? '')
  const [content, setContent] = useState(initial?.content ?? '')
  const [notice, setNotice] = useState(initial?.notice ?? false)
  const [attachments, setAttachments] = useState<UploadedFile[]>(initial?.attachments ?? [])
  const [inlineFiles, setInlineFiles] = useState<UploadedFile[]>([])
  const [uploading, setUploading] = useState(false)
  const attachmentInputRef = useRef<HTMLInputElement>(null)

  const save = useMutation({
    mutationFn: (payload: PostUpsertPayload) =>
      isEdit ? postApi.update(postId, payload) : postApi.create(slug, payload),
    onSuccess: async (saved) => {
      toast.success(isEdit ? '글을 수정했습니다.' : '글을 등록했습니다.')
      await queryClient.invalidateQueries({ queryKey: postKeys.all })
      navigate(`/boards/${slug}/posts/${saved.id}`, { replace: true })
    },
    onError: (error: ApiError) => toast.error(error.message),
  })

  const uploadAttachment = async (file: File) => {
    setUploading(true)
    try {
      const uploaded = await fileApi.upload(file)
      setAttachments((prev) => [...prev, uploaded])
    } catch (error) {
      toast.error(toApiError(error).message)
    } finally {
      setUploading(false)
    }
  }

  const submit = () => {
    if (!title.trim()) {
      toast.error('제목을 입력해 주세요.')
      return
    }
    if (!content.trim() || content === '<p></p>') {
      toast.error('내용을 입력해 주세요.')
      return
    }

    // 본문에 남아 있는 이미지만 추려 보낸다(중간에 지운 이미지는 제외).
    const usedInline = inlineFiles.filter((file) => content.includes(file.url))
    const fileIds = [...attachments.map((file) => file.id), ...usedInline.map((file) => file.id)]

    save.mutate({
      title: title.trim(),
      content,
      notice,
      fileIds,
      inlineFileIds: usedInline.map((file) => file.id),
    })
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">{isEdit ? '글 수정' : '글쓰기'}</h1>

      <div className="space-y-2">
        <Label htmlFor="title">제목</Label>
        <Input
          id="title"
          value={title}
          maxLength={200}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="제목을 입력하세요."
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="content">내용</Label>
        <RichTextEditor
          value={content}
          onChange={setContent}
          onImageUploaded={(file) => setInlineFiles((prev) => [...prev, file])}
        />
        <p className="text-xs text-muted-foreground">
          이미지는 붙여넣거나 끌어다 놓으면 바로 업로드됩니다.
        </p>
      </div>

      {attachmentEnabled && (
        <div className="space-y-2">
          <Label>첨부 파일</Label>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={uploading}
              onClick={() => attachmentInputRef.current?.click()}
            >
              <Paperclip className="mr-1 size-4" />
              {uploading ? '업로드 중…' : '파일 추가'}
            </Button>
            <input
              ref={attachmentInputRef}
              type="file"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (file) void uploadAttachment(file)
                event.target.value = ''
              }}
            />
          </div>

          {attachments.length > 0 && (
            <ul className="space-y-1">
              {attachments.map((file) => (
                <li key={file.id} className="flex items-center gap-2 text-sm">
                  <span>{file.originalName}</span>
                  <span className="text-xs text-muted-foreground">{formatFileSize(file.size)}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-6"
                    aria-label={`${file.originalName} 첨부 제거`}
                    onClick={() => setAttachments((prev) => prev.filter((f) => f.id !== file.id))}
                  >
                    <X className="size-3.5" />
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {isAdmin && (
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={notice}
            onChange={(event) => setNotice(event.target.checked)}
          />
          공지로 등록 (목록 상단에 고정됩니다)
        </label>
      )}

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => navigate(-1)}>
          취소
        </Button>
        <Button onClick={submit} disabled={save.isPending}>
          {save.isPending ? '저장 중…' : isEdit ? '수정' : '등록'}
        </Button>
      </div>
    </div>
  )
}
