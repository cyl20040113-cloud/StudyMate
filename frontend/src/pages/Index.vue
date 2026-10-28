<template>
  <div class="page home-page">
    <section class="hero-card">
      <span class="eyebrow">StudyMate · 伴学星球</span>
      <h1>找到和你一起进步的伙伴</h1>
      <p>按兴趣标签发现伙伴，也可以开启匹配模式寻找与你更相似的用户。</p>

      <van-button
        round
        type="primary"
        icon="search"
        @click="router.push('/search')"
      >
        按标签找伙伴
      </van-button>
    </section>

    <section class="mode-card">
      <div>
        <h2>推荐匹配</h2>
        <p>
          {{
            isMatchMode
              ? "正在优先展示与你标签更相似的伙伴"
              : "当前展示推荐用户"
          }}
        </p>
      </div>

      <van-switch v-model="isMatchMode" size="24" />
    </section>
  </div>

  <UserCardList :user-list="userList" :loading="loading" />

  <van-empty
    v-if="!loading && userList.length === 0"
    description="暂时没有可推荐的伙伴"
    class="empty-wrap"
  />
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { Toast } from "vant";
import { useRouter } from "vue-router";

import UserCardList from "../components/UserCardList.vue";
import myAxios from "../plugins/myAxios";
import { normalizeUser } from "../utils/user";

import type { RawUser, UserType } from "../models/user";
import type { PageResult } from "../types/api";

const router = useRouter();
const userList = ref<UserType[]>([]);
const isMatchMode = ref(false);
const loading = ref(false);

const loadData = async () => {
  loading.value = true;

  try {
    if (isMatchMode.value) {
      const res = await myAxios.get<RawUser[]>("/user/match", {
        params: { num: 10 },
      });

      if (res.code === 0) {
        userList.value = (res.data || []).map(normalizeUser);
      } else {
        Toast.fail(res.description || res.msg || "匹配失败");
      }
    } else {
      const res = await myAxios.get<PageResult<RawUser>>("/user/recommend", {
        params: { pageSize: 10, pageNum: 1 },
      });

      if (res.code === 0) {
        userList.value = (res.data?.records || []).map(normalizeUser);
      } else {
        Toast.fail(res.description || res.msg || "加载推荐失败");
      }
    }
  } catch (error) {
    console.error("首页用户加载失败：", error);
  } finally {
    loading.value = false;
  }
};

watch(isMatchMode, loadData, { immediate: true });
</script>

<style scoped>
.home-page {
  padding: 16px 16px 0;
}

.hero-card {
  padding: 22px;
  color: white;
  text-align: left;
  border-radius: 22px;
  background: linear-gradient(135deg, #6676f6, #8c74f7);
  box-shadow: 0 12px 30px rgba(103, 118, 246, 0.24);
}

.eyebrow {
  font-size: 12px;
  opacity: 0.82;
}

.hero-card h1 {
  margin: 8px 0 9px;
  font-size: 24px;
  line-height: 1.35;
}

.hero-card p {
  margin: 0 0 18px;
  font-size: 13px;
  line-height: 1.7;
  opacity: 0.9;
}

.hero-card :deep(.van-button) {
  color: #6f7bf7;
  background: white;
  border: 0;
}

.mode-card {
  margin-top: 12px;
  padding: 15px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  background: #fff;
  border-radius: 16px;
}

.mode-card h2 {
  margin: 0;
  font-size: 16px;
}

.mode-card p {
  margin: 4px 0 0;
  color: #8a94a3;
  font-size: 12px;
}

.empty-wrap {
  padding: 20px;
}
</style>
