<template>
  <div class="search-result-page">
    <UserCardList :user-list="userList" :loading="loading" />

    <div v-if="loading" class="loading-text">正在搜索伙伴...</div>

    <van-empty
      v-if="!loading && userList.length === 0"
      description="搜索结果为空"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { Toast } from "vant";
import qs from "qs";

import myAxios from "../plugins/myAxios";
import UserCardList from "../components/UserCardList.vue";
import { normalizeUser } from "../utils/user";

import type { RawUser, UserType } from "../models/user";
import type { PageResult } from "../types/api";

const route = useRoute();
const userList = ref<UserType[]>([]);
const loading = ref(false);

const tags = computed<string[]>(() => {
  const routeTags = route.query.tags;

  if (Array.isArray(routeTags)) {
    return routeTags.filter((tag): tag is string => typeof tag === "string");
  }

  return typeof routeTags === "string" ? [routeTags] : [];
});

const loadUsers = async () => {
  if (!tags.value.length) {
    Toast("请选择至少一个标签");
    return;
  }

  loading.value = true;

  try {
    const res = await myAxios.get<PageResult<RawUser>>("/user/search/tags", {
      params: {
        tagNameList: tags.value,
        pageNum: 1,
        pageSize: 8,
      },
      paramsSerializer: {
        serialize: (params) => qs.stringify(params, { indices: false }),
      },
    });

    if (res.code === 0) {
      userList.value = (res.data?.records || []).map(normalizeUser);
    } else {
      Toast.fail(res.description || res.msg || "搜索失败");
    }
  } catch (error) {
    console.error("搜索用户失败：", error);
  } finally {
    loading.value = false;
  }
};

onMounted(loadUsers);
</script>

<style scoped>
.search-result-page {
  padding: 12px;
}

.loading-text {
  padding: 20px;
  color: #969799;
  font-size: 14px;
  text-align: center;
}
</style>
