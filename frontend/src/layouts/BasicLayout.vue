<template>
  <div class="basic-layout">
    <!-- 登录 / 注册页隐藏顶部导航 -->
    <van-nav-bar
      v-if="!hideLayout"
      :title="title"
      left-arrow
      @click-left="onClickLeft"
    >
      <template #right>
        <van-icon name="search" size="18" @click="onClickRight" />
      </template>
    </van-nav-bar>

    <!-- 页面内容 -->
    <div id="content" :class="{ 'has-tabbar': !hideLayout }">
      <router-view />
    </div>

    <!-- 登录 / 注册页隐藏底部导航 -->
    <van-tabbar v-if="!hideLayout" route>
      <van-tabbar-item to="/" icon="home-o" name="index">
        主页
      </van-tabbar-item>

      <van-tabbar-item to="/team" icon="search" name="team">
        队伍
      </van-tabbar-item>

      <van-tabbar-item to="/user" icon="friends-o" name="user">
        个人
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const DEFAULT_TITLE = "伴学星球";

const title = computed(() => {
  return typeof route.meta.title === "string"
    ? route.meta.title
    : DEFAULT_TITLE;
});

const hideLayout = computed(() => {
  return route.meta.hideLayout === true;
});

const onClickLeft = () => {
  router.back();
};

const onClickRight = () => {
  router.push("/search");
};
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

/* 普通页面内容区域 */
#content {
  box-sizing: border-box;
}

/* 有顶部导航 + 底部 Tabbar 的页面 */
#content.has-tabbar {
  min-height: calc(100vh - 46px);
  padding-bottom: 50px;
  background: #f6f8fc;
}
</style>
