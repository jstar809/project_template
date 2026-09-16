import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import { EditorContent, useEditor, type Editor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Bold, Code, Italic, List, ListOrdered, Quote, Strikethrough } from 'lucide-react'
import { useCallback, useRef, useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { fileApi, type UploadedFile } from '@/features/file/api'
import { toApiError } from '@/shared/api/errors'
import { cn } from '@/lib/utils'

interface RichTextEditorProps {
  value: string
  onChange: (html: string) => void
  /** 본문에 삽입된 이미지가 업로드될 때마다 알려준다(작성 시 fileIds로 보낸다). */
  onImageUploaded: (file: UploadedFile) => void
  placeholder?: string
}

/**
 * TipTap 기반 리치 텍스트 에디터.
 *
 * 이미지를 붙여넣거나 끌어다 놓으면 곧바로 서버에 업로드하고, 반환된 URL을 본문에 넣는다.
 * (글을 저장하기 전에 올라가므로, 저장하지 않은 파일은 서버가 24시간 뒤 정리한다.)
 */
export function RichTextEditor({
  value,
  onChange,
  onImageUploaded,
  placeholder = '내용을 입력하세요.',
}: RichTextEditorProps) {
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const uploadAndInsert = useCallback(
    async (editor: Editor, file: File) => {
      setUploading(true)
      try {
        const uploaded = await fileApi.upload(file)
        editor.chain().focus().setImage({ src: uploaded.url, alt: uploaded.originalName }).run()
        onImageUploaded(uploaded)
      } catch (error) {
        toast.error(toApiError(error).message)
      } finally {
        setUploading(false)
      }
    },
    [onImageUploaded],
  )

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({ HTMLAttributes: { class: 'max-w-full rounded-md' } }),
      Link.configure({ openOnClick: false, HTMLAttributes: { rel: 'noopener noreferrer' } }),
      Placeholder.configure({ placeholder }),
    ],
    content: value,
    onUpdate: ({ editor: instance }) => onChange(instance.getHTML()),
    editorProps: {
      attributes: {
        class: 'min-h-64 w-full px-3 py-2 focus:outline-none',
      },
      handlePaste: (_view, event) => {
        const files = Array.from(event.clipboardData?.files ?? [])
        const image = files.find((file) => file.type.startsWith('image/'))
        if (!image || !editor) return false
        event.preventDefault()
        void uploadAndInsert(editor, image)
        return true
      },
      handleDrop: (_view, event) => {
        const files = Array.from((event as DragEvent).dataTransfer?.files ?? [])
        const image = files.find((file) => file.type.startsWith('image/'))
        if (!image || !editor) return false
        event.preventDefault()
        void uploadAndInsert(editor, image)
        return true
      },
    },
  })

  if (!editor) {
    return <div className="h-64 rounded-md border" />
  }

  return (
    <div className="rounded-md border">
      <div className="flex flex-wrap items-center gap-1 border-b p-1">
        <ToolbarButton
          label="굵게"
          active={editor.isActive('bold')}
          onClick={() => editor.chain().focus().toggleBold().run()}
        >
          <Bold className="size-4" />
        </ToolbarButton>
        <ToolbarButton
          label="기울임"
          active={editor.isActive('italic')}
          onClick={() => editor.chain().focus().toggleItalic().run()}
        >
          <Italic className="size-4" />
        </ToolbarButton>
        <ToolbarButton
          label="취소선"
          active={editor.isActive('strike')}
          onClick={() => editor.chain().focus().toggleStrike().run()}
        >
          <Strikethrough className="size-4" />
        </ToolbarButton>
        <ToolbarButton
          label="글머리 기호"
          active={editor.isActive('bulletList')}
          onClick={() => editor.chain().focus().toggleBulletList().run()}
        >
          <List className="size-4" />
        </ToolbarButton>
        <ToolbarButton
          label="번호 매기기"
          active={editor.isActive('orderedList')}
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
        >
          <ListOrdered className="size-4" />
        </ToolbarButton>
        <ToolbarButton
          label="인용"
          active={editor.isActive('blockquote')}
          onClick={() => editor.chain().focus().toggleBlockquote().run()}
        >
          <Quote className="size-4" />
        </ToolbarButton>
        <ToolbarButton
          label="코드"
          active={editor.isActive('codeBlock')}
          onClick={() => editor.chain().focus().toggleCodeBlock().run()}
        >
          <Code className="size-4" />
        </ToolbarButton>

        <span className="mx-1 h-4 w-px bg-border" />

        <Button
          type="button"
          variant="ghost"
          size="sm"
          disabled={uploading}
          onClick={() => fileInputRef.current?.click()}
        >
          {uploading ? '업로드 중…' : '이미지'}
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) void uploadAndInsert(editor, file)
            event.target.value = ''
          }}
        />
      </div>

      <EditorContent editor={editor} />
    </div>
  )
}

function ToolbarButton({
  label,
  active,
  onClick,
  children,
}: {
  label: string
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      aria-label={label}
      aria-pressed={active}
      className={cn('size-8', active && 'bg-accent text-accent-foreground')}
      onClick={onClick}
    >
      {children}
    </Button>
  )
}
