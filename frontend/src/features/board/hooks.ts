import { useQuery } from '@tanstack/react-query'
import { boardApi, boardKeys } from './api'

/** 헤더 메뉴와 홈에서 함께 쓰므로 캐시를 넉넉히 둔다. */
export function useBoards() {
  return useQuery({
    queryKey: boardKeys.list(),
    queryFn: boardApi.list,
    staleTime: 5 * 60 * 1000,
  })
}

export function useBoard(slug: string) {
  return useQuery({
    queryKey: boardKeys.detail(slug),
    queryFn: () => boardApi.detail(slug),
    enabled: Boolean(slug),
  })
}
