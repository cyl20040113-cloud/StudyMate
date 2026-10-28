<template>
  <div class="user-page">
    <div v-if="loading" class="loading-wrapper">
      <van-loading vertical size="24">正在加载个人信息...</van-loading>
    </div>

    <template v-else-if="user">
      <section class="profile-card">
        <div class="profile-header">
          <van-image
            round
            width="72"
            height="72"
            :src="user.avatarUrl"
            class="avatar"
          >
            <template #error>
              <div class="avatar-placeholder">
                {{ getAvatarText(user.username) }}
              </div>
            </template>

            <template #loading>
              <van-loading size="20" />
            </template>
          </van-image>

          <div class="profile-info">
            <div class="username-row">
              <h2>{{ user.username || "未设置昵称" }}</h2>

              <van-tag v-if="user.gender === 0" plain type="primary"
                >男</van-tag
              >
              <van-tag v-else-if="user.gender === 1" plain type="danger"
                >女</van-tag
              >
            </div>

            <div class="account">@{{ user.userAccount }}</div>
            <div v-if="user.planetCode" class="planet-code">
              星球编号：{{ user.planetCode }}
            </div>
          </div>
        </div>

        <van-button
          round
          block
          plain
          type="primary"
          class="edit-button"
          @click="router.push('/user/update')"
        >
          编辑个人资料
        </van-button>
      </section>

      <section class="content-card">
        <div class="section-title">个人简介</div>
        <div class="profile-text">
          {{ user.profile || "还没有填写个人简介，介绍一下自己吧～" }}
        </div>
      </section>

      <section class="content-card">
        <div class="section-title">我的标签</div>

        <div v-if="user.tags?.length" class="tag-list">
          <van-tag
            v-for="tag in user.tags"
            :key="tag"
            plain
            type="primary"
            size="medium"
          >
            {{ tag }}
          </van-tag>
        </div>

        <div v-else class="empty-text">暂时还没有设置标签</div>
      </section>

      <section class="content-card menu-card">
        <div class="section-title menu-title">我的队伍</div>

        <van-cell
          title="我创建的队伍"
          label="查看自己创建的学习队伍"
          icon="friends-o"
          is-link
          to="/team/create"
        />

        <van-cell
          title="我加入的队伍"
          label="查看当前已经加入的队伍"
          icon="cluster-o"
          is-link
          to="/team/join"
        />
      </section>

      <section class="content-card menu-card">
        <div class="section-title menu-title">账号信息</div>

        <van-cell title="手机号" :value="user.phone || '未设置'" />
        <van-cell title="邮箱" :value="user.email || '未设置'" />
        <van-cell
          title="注册时间"
          :value="user.createTime ? formatDateTime(user.createTime) : '未知'"
        />
      </section>

      <div class="logout-wrapper">
        <van-button
          round
          block
          plain
          type="danger"
          :loading="loggingOut"
          :disabled="loggingOut"
          @click="doLogout"
        >
          退出登录
        </van-button>
      </div>
    </template>

    <van-empty v-else description="暂时无法获取用户信息">
      <van-button round type="primary" @click="loadCurrentUser">
        重新加载
      </van-button>
    </van-empty>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Dialog, Toast } from "vant";

import myAxios from "../plugins/myAxios";
import { getCurrentUser } from "../services/user";
import { clearCurrentUserState } from "../states/user";
import { formatDateTime } from "../utils/date";
import type { UserDetailType } from "../models/user";

const router = useRouter();
const user = ref<UserDetailType | null>(null);
const loading = ref(false);
const loggingOut = ref(false);

const loadCurrentUser = async () => {
  loading.value = true;

  try {
    const currentUser = await getCurrentUser();

    if (!currentUser) {
      user.value = null;
      Toast.fail("获取用户信息失败");
      return;
    }

    user.value = currentUser;
  } catch (error) {
    console.error("获取用户信息失败：", error);
    user.value = null;
  } finally {
    loading.value = false;
  }
};

const getAvatarText = (username?: string) => username?.charAt(0) || "伴";

const doLogout = async () => {
  try {
    await Dialog.confirm({
      title: "退出登录",
      message: "确定要退出当前账号吗？",
    });
  } catch {
    return;
  }

  if (loggingOut.value) return;
  loggingOut.value = true;

  try {
    const res = await myAxios.post<number>("/user/logout");

    if (res.code === 0) {
      clearCurrentUserState();
      user.value = null;

      Toast.success("已退出登录");
      router.replace("/user/login");
    } else {
      Toast.fail(res.description || res.msg || "退出登录失败");
    }
  } catch (error) {
    console.error("退出登录失败：", error);
  } finally {
    loggingOut.value = false;
  }
};

onMounted(loadCurrentUser);
</script>

<style scoped>
.user-page {
  min-height: calc(100vh - 96px);
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  background: #f6f8fc;
}

.loading-wrapper {
  padding: 80px 0;
}

.profile-card {
  padding: 20px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 6px 22px rgba(0, 0, 0, 0.05);
}

.profile-header {
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
  color: #6f7bf7;
  font-size: 26px;
  font-weight: 600;
  background: #eef0ff;
}

.profile-info {
  margin-left: 15px;
  flex: 1;
  min-width: 0;
}

.username-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.username-row h2 {
  margin: 0;
  color: #252a34;
  font-size: 21px;
}

.account,
.planet-code {
  margin-top: 5px;
  color: #969799;
  font-size: 13px;
}

.planet-code {
  margin-top: 4px;
  font-size: 12px;
}

.edit-button {
  margin-top: 18px;
}

.content-card {
  margin-top: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 18px;
}

.section-title {
  margin-bottom: 12px;
  color: #323233;
  font-size: 15px;
  font-weight: 600;
}

.profile-text {
  color: #646566;
  font-size: 13px;
  line-height: 1.8;
}

.empty-text {
  color: #c8c9cc;
  font-size: 13px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.menu-card {
  padding: 0;
  overflow: hidden;
}

.menu-title {
  margin: 0;
  padding: 16px 16px 8px;
}

.logout-wrapper {
  margin-top: auto;
  padding-top: 18px;
  padding-bottom: 10px;
}
</style>
