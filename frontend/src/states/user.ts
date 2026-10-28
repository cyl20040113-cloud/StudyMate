import type {
  UserDetailType
} from "../models/user";


/**
 * 当前登录用户
 *
 * undefined 表示：
 * 当前还没有缓存用户信息
 */
let currentUser:
  UserDetailType | undefined;


/**
 * 保存当前登录用户
 */
const setCurrentUserState = (
  user: UserDetailType
) => {

  currentUser = user;

};


/**
 * 获取当前登录用户
 */
const getCurrentUserState =
  (): UserDetailType | undefined => {

    return currentUser;

  };


/**
 * 清除当前登录用户
 *
 * 退出登录时使用
 */
const clearCurrentUserState = () => {

  currentUser = undefined;

};


export {
  setCurrentUserState,
  getCurrentUserState,
  clearCurrentUserState
};