import { http, HttpResponse } from 'msw'
import { BASE } from './handlers'

export const testBoard = {
  id: 1,
  slug: 'free',
  name: '자유게시판',
  description: '자유롭게 글을 쓰는 게시판입니다.',
  commentEnabled: true,
  attachmentEnabled: true,
  canWrite: true,
}

export const noticePost = {
  id: 10,
  title: '공지사항입니다',
  authorNickname: '관리자',
  notice: true,
  viewCount: 100,
  commentCount: 0,
  hasAttachment: false,
  createdAt: '2026-09-15T00:00:00Z',
}

export const normalPost = {
  id: 11,
  title: '첫 번째 글',
  authorNickname: '테스터',
  notice: false,
  viewCount: 5,
  commentCount: 2,
  hasAttachment: true,
  createdAt: '2026-09-16T00:00:00Z',
}

export const postDetail = {
  id: 11,
  board: { slug: 'free', name: '자유게시판' },
  title: '첫 번째 글',
  content: '<p>본문 내용입니다</p>',
  author: { id: 1, nickname: '테스터' },
  notice: false,
  viewCount: 6,
  commentCount: 2,
  attachments: [
    {
      id: 5,
      url: '/api/v1/files/5',
      originalName: '자료.pdf',
      contentType: 'application/pdf',
      size: 2048,
    },
  ],
  canEdit: true,
  canDelete: true,
  createdAt: '2026-09-16T00:00:00Z',
  updatedAt: '2026-09-16T00:00:00Z',
}

export const comments = [
  {
    id: 100,
    content: '첫 댓글입니다',
    authorId: 2,
    authorNickname: '다른사람',
    deleted: false,
    hidden: false,
    canEdit: false,
    canDelete: false,
    createdAt: '2026-09-16T01:00:00Z',
    updatedAt: '2026-09-16T01:00:00Z',
    replies: [
      {
        id: 101,
        content: '답글입니다',
        authorId: 1,
        authorNickname: '테스터',
        deleted: false,
        hidden: false,
        canEdit: true,
        canDelete: true,
        createdAt: '2026-09-16T02:00:00Z',
        updatedAt: '2026-09-16T02:00:00Z',
        replies: [],
      },
    ],
  },
]

/** 게시판/게시글/댓글 기본 핸들러. 필요한 테스트에서 server.use()로 덮어쓴다. */
export const cmsHandlers = [
  http.get(`${BASE}/boards`, () => HttpResponse.json([testBoard])),
  http.get(`${BASE}/boards/free`, () => HttpResponse.json(testBoard)),

  http.get(`${BASE}/boards/free/posts`, ({ request }) => {
    const url = new URL(request.url)
    const keyword = url.searchParams.get('keyword')

    // 검색어가 있으면 제목이 맞는 글만 돌려준다.
    const matched = keyword
      ? [normalPost].filter((post) => post.title.includes(keyword))
      : [normalPost]

    return HttpResponse.json({
      notices: keyword ? [] : [noticePost],
      page: {
        content: matched,
        page: 0,
        size: 20,
        totalElements: matched.length,
        totalPages: matched.length > 0 ? 1 : 0,
        hasNext: false,
      },
    })
  }),

  http.get(`${BASE}/posts/11`, () => HttpResponse.json(postDetail)),
  http.get(`${BASE}/posts/11/comments`, () => HttpResponse.json(comments)),
]
