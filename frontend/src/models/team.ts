import type { UserType } from './user';

export type TeamType = {
  /**
   * 队伍 id
   */
  id: number;

  /**
   * 队伍名称
   */
  name: string;

  /**
   * 队伍描述
   */
  description?: string;

  /**
   * 最大人数
   */
  maxNum: number;

  /**
   * 过期时间
   *
   * 后端 Date 转成 JSON 后，
   * 前端收到的是字符串
   */
  expireTime?: string;

  /**
   * 队长用户 id
   */
  userId: number;

  /**
   * 队伍状态
   *
   * 0 = 公开
   * 1 = 私有
   * 2 = 加密
   */
  status: 0 | 1 | 2;

  /**
   * 创建时间
   */
  createTime?: string;

  /**
   * 更新时间
   */
  updateTime?: string;

  /**
   * 创建队伍的用户
   */
  createUser?: UserType;

  /**
   * 当前已经加入的人数
   */
  hasJoinNum?: number;

  /**
   * 当前登录用户是否已经加入
   */
  hasJoin: boolean;
};