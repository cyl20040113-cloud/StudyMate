import type {
  RawUser,
  UserType
} from '../models/user';

export const normalizeTags = (
  tags?: string | string[] | null
): string[] => {

  if (Array.isArray(tags)) {
    return tags;
  }

  if (!tags) {
    return [];
  }

  try {
    const parsed = JSON.parse(tags);

    return Array.isArray(parsed)
      ? parsed
      : [];
  } catch (error) {
    console.error(
      '标签解析失败：',
      tags,
      error
    );

    return [];
  }
};

export const normalizeUser = (
  user: RawUser
): UserType => {
  return {
    ...user,

    tags: normalizeTags(user.tags),
  };
};