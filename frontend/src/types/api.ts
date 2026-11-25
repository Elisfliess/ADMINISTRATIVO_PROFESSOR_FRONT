/** RFC 9457 Problem Details with the API's documented extensions. */
export interface ProblemDetails {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  code?: string
  requestId?: string
  errors?: ValidationIssue[]
}

export interface ValidationIssue {
  field: string
  code: string
  message: string
}

export interface ApiError extends Error {
  status: number
  problem: ProblemDetails | null
}
