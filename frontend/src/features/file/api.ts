import { apiClient } from '@/shared/api/client'

export interface UploadedFile {
  id: number
  url: string
  originalName: string
  contentType: string
  size: number
}

export const fileApi = {
  /** 파일 하나를 업로드하고 메타데이터를 돌려준다. */
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<UploadedFile>('/files', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((res) => res.data)
  },
}

/** 바이트를 사람이 읽는 크기로 바꾼다. */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
