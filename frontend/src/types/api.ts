// src/types/api.ts

export interface BaseResponse<T = unknown> {
  code: number;
  data: T;
  msg?: string;
  description?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}