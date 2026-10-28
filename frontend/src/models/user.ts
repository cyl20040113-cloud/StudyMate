/**
 * 后端返回的公开用户原始数据
 */
export type RawUser = {
  id: number;
  username: string;
  avatarUrl?: string;
  gender?: number;
  profile?: string;
  planetCode?: string;

  /**
   * 后端可能返回 JSON 字符串
   */
  tags?: string | string[] | null;
};


/**
 * 前端页面真正使用的公开用户
 */
export type UserType = {
  id: number;
  username: string;
  avatarUrl?: string;
  gender?: number;
  profile?: string;
  planetCode?: string;

  /**
   * 到前端以后统一保证是数组
   */
  tags: string[];
};


/**
 * 当前登录用户的完整信息
 */
export type UserDetailType =
  UserType & {

    userAccount: string;

    phone?: string;

    email?: string;

    userStatus?: number;

    userRole?: number;

    createTime?: string;

    updateTime?: string;

  };


/**
 * 后端返回的当前用户原始数据
 */
export type RawUserDetailType =
  Omit<UserDetailType, "tags"> & {

    tags?: string | string[] | null;

  };