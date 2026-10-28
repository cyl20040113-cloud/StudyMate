<template>
  <div class="user-card-list">
    <!-- 加载中的骨架屏 -->
    <template v-if="loading">
      <div v-for="index in 3" :key="index" class="skeleton-card">
        <van-skeleton title avatar :row="3" />
      </div>
    </template>

    <!-- 用户列表 -->
    <template v-else>
      <div v-for="user in userList" :key="user.id" class="user-card">
        <!-- 用户头像 -->
        <div class="user-header">
          <van-image
            round
            width="56"
            height="56"
            :src="user.avatarUrl"
            class="avatar"
          >
            <!-- 图片加载失败 -->
            <template #error>
              <div class="avatar-placeholder">
                {{ getAvatarText(user.username) }}
              </div>
            </template>

            <!-- 图片加载中 -->
            <template #loading>
              <van-loading type="spinner" size="18" />
            </template>
          </van-image>

          <!-- 用户基础信息 -->
          <div class="user-info">
            <div class="username-row">
              <span class="username">
                {{ user.username || "未设置昵称" }}
              </span>

              <!-- 性别 -->
              <van-tag v-if="user.gender === 0" type="primary" plain>
                男
              </van-tag>

              <van-tag v-else-if="user.gender === 1" type="danger" plain>
                女
              </van-tag>
            </div>

            <div v-if="user.planetCode" class="planet-code">
              星球编号：{{ user.planetCode }}
            </div>
          </div>
        </div>

        <!-- 用户简介 -->
        <div class="profile">
          {{ user.profile || "这个伙伴还没有填写个人简介" }}
        </div>

        <!-- 用户标签 -->
        <div v-if="user.tags && user.tags.length > 0" class="tag-list">
          <van-tag
            v-for="tag in user.tags"
            :key="tag"
            plain
            type="primary"
            class="user-tag"
          >
            {{ tag }}
          </van-tag>
        </div>

        <!-- 没有标签 -->
        <div v-else class="no-tags">暂无标签</div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { UserType } from "../models/user";

/**
 * UserCardList 接收的参数
 */
interface UserCardListProps {
  /**
   * 用户列表
   */
  userList: UserType[];

  /**
   * 是否正在加载
   */
  loading?: boolean;
}

/**
 * 设置 props 默认值
 */
withDefaults(defineProps<UserCardListProps>(), {
  userList: () => [],
  loading: false,
});

/**
 * 用户没有头像时，
 * 使用昵称第一个字符作为占位符
 */
const getAvatarText = (username?: string): string => {
  if (!username) {
    return "伴";
  }

  return username.charAt(0);
};
</script>

<style scoped>
.user-card-list {
  padding: 12px 16px 20px;
}

/* =========================
   用户卡片
   ========================= */

.user-card {
  margin-bottom: 12px;

  padding: 16px;

  background: #ffffff;

  border-radius: 18px;

  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
}

/* =========================
   顶部用户信息
   ========================= */

.user-header {
  display: flex;

  align-items: center;
}

.avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;

  display: flex;

  align-items: center;
  justify-content: center;

  background: #eef0ff;

  color: #6f7bf7;

  font-size: 20px;

  font-weight: 600;
}

.user-info {
  margin-left: 12px;

  flex: 1;

  min-width: 0;
}

.username-row {
  display: flex;

  align-items: center;

  gap: 7px;
}

.username {
  color: #252a34;

  font-size: 16px;

  font-weight: 600;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.planet-code {
  margin-top: 5px;

  color: #969799;

  font-size: 12px;
}

/* =========================
   个人简介
   ========================= */

.profile {
  margin-top: 13px;

  color: #646566;

  font-size: 13px;

  line-height: 1.7;
}

/* =========================
   标签
   ========================= */

.tag-list {
  margin-top: 12px;

  display: flex;

  flex-wrap: wrap;

  gap: 7px;
}

.user-tag {
  margin: 0;
}

.no-tags {
  margin-top: 12px;

  color: #c8c9cc;

  font-size: 12px;
}

/* =========================
   骨架屏
   ========================= */

.skeleton-card {
  margin-bottom: 12px;

  padding: 18px 16px;

  background: white;

  border-radius: 18px;
}
</style>
