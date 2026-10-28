import { createApp } from "vue";
import App from "./App.vue";
import * as VueRouter from "vue-router";

import routes from "./config/route";
import { getCurrentUserState } from "./states/user";
import { getCurrentUser } from "./services/user";

import { Button, NavBar, Icon, Tabbar, TabbarItem, Dialog } from "vant";

import "vant/lib/index.css";
import "./global.css";

// 重置 Dialog 默认配置
Dialog.resetDefaultOptions();

const app = createApp(App);

// Vant 组件
app.use(Button);
app.use(NavBar);
app.use(Icon);
app.use(Tabbar);
app.use(TabbarItem);

// 创建路由
const router = VueRouter.createRouter({
  history: VueRouter.createWebHashHistory(),
  routes,
});

// 全局登录拦截
router.beforeEach(async (to) => {
  // 设置网页标题
  document.title =
    typeof to.meta.title === "string"
      ? `${to.meta.title} - StudyMate`
      : "StudyMate - 伴学星球";

  // 不需要登录的页面直接放行
  if (to.meta.requiresAuth !== true) {
    return true;
  }

  // 先检查前端是否已有用户
  let currentUser = getCurrentUserState();

  // 没有则请求后端恢复登录状态
  if (!currentUser) {
    currentUser = (await getCurrentUser()) ?? undefined;
  }

  // 已登录
  if (currentUser) {
    return true;
  }

  // 未登录，跳转登录页
  return {
    path: "/user/login",
    query: {
      redirect: to.fullPath,
    },
  };
});

app.use(router);
app.mount("#app");
