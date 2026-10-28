import type { RouteRecordRaw } from "vue-router";

import Index from "../pages/Index.vue";
import SearchPage from "../pages/SearchPage.vue";
import SearchResultPage from "../pages/SearchResultPage.vue";
import TeamPage from "../pages/TeamPage.vue";
import TeamAddPage from "../pages/TeamAddPage.vue";
import TeamUpdatePage from "../pages/TeamUpdatePage.vue";
import TeamJoinPage from "../pages/TeamJoinPage.vue";
import TeamCreatePage from "../pages/TeamCreatePage.vue";
import UserPage from "../pages/UserPage.vue";
import UserUpdatePage from "../pages/UserUpdatePage.vue";
import UserLoginPage from "../pages/UserLoginPage.vue";
import UserRegisterPage from "../pages/UserRegisterPage.vue";

const routes: RouteRecordRaw[] = [
  { path: "/", component: Index, meta: { title: "首页", requiresAuth: true } },
  {
    path: "/search",
    component: SearchPage,
    meta: { title: "找伙伴", requiresAuth: true },
  },
  {
    path: "/user/list",
    component: SearchResultPage,
    meta: { title: "用户列表", requiresAuth: true },
  },

  {
    path: "/team",
    component: TeamPage,
    meta: { title: "查找队伍", requiresAuth: true },
  },
  {
    path: "/team/add",
    component: TeamAddPage,
    meta: { title: "添加队伍", requiresAuth: true },
  },
  {
    path: "/team/update",
    component: TeamUpdatePage,
    meta: { title: "更新队伍", requiresAuth: true },
  },
  {
    path: "/team/join",
    component: TeamJoinPage,
    meta: { title: "我加入的队伍", requiresAuth: true },
  },
  {
    path: "/team/create",
    component: TeamCreatePage,
    meta: { title: "我创建的队伍", requiresAuth: true },
  },

  {
    path: "/user",
    component: UserPage,
    meta: { title: "用户中心", requiresAuth: true },
  },
  {
    path: "/user/update",
    component: UserUpdatePage,
    meta: { title: "编辑资料", requiresAuth: true },
  },

  {
    path: "/user/login",
    component: UserLoginPage,
    meta: { title: "用户登录", requiresAuth: false, hideLayout: true },
  },
  {
    path: "/user/register",
    component: UserRegisterPage,
    meta: { title: "用户注册", requiresAuth: false, hideLayout: true },
  },

  { path: "/:pathMatch(.*)*", redirect: "/" },
];

export default routes;
