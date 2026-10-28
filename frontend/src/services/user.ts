import myAxios from "../plugins/myAxios";

import { setCurrentUserState } from "../states/user";

import { normalizeTags } from "../utils/user";

import type { RawUserDetailType, UserDetailType } from "../models/user";

/**
 * 获取当前登录用户
 */
export const getCurrentUser = async (): Promise<UserDetailType | null> => {
  try {
    const res = await myAxios.get<RawUserDetailType>("/user/current");

    if (res.code !== 0) {
      return null;
    }

    /**
     * 把后端 tags JSON
     * 转换成 string[]
     */
    const user: UserDetailType = {
      ...res.data,

      tags: normalizeTags(res.data.tags),
    };

    /**
     * 保存到前端用户状态
     */
    setCurrentUserState(user);

    return user;
  } catch (error) {
    console.error("获取当前用户失败：", error);

    return null;
  }
};
