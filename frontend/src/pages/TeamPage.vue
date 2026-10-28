<template>
  <div class="team-page">
    <section class="team-header">
      <div class="header-text">
        <h2>学习队伍</h2>
        <p>找到伙伴，一起学习、刷题和做项目</p>
      </div>

      <van-button
        round
        size="small"
        type="primary"
        icon="plus"
        class="create-button"
        @click="doCreateTeam"
      >
        新建
      </van-button>
    </section>

    <div class="search-wrapper">
      <van-search
        v-model="searchText"
        placeholder="搜索队伍名称或描述"
        shape="round"
        clearable
        class="team-search"
        @search="loadTeams"
        @clear="onClear"
      />
    </div>

    <van-tabs v-model:active="active" class="team-tabs" @change="onTabChange">
      <van-tab title="公开队伍" name="public" />
      <van-tab title="加密队伍" name="secret" />
    </van-tabs>

    <div v-if="loading" class="loading">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <TeamCardList v-else :team-list="teamList" @refresh="loadTeams" />

    <van-empty
      v-if="!loading && !teamList.length"
      :description="searchText ? '没有找到符合条件的队伍' : '暂时还没有队伍'"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Toast } from "vant";
import TeamCardList from "../components/TeamCardList.vue";
import myAxios from "../plugins/myAxios";
import type { TeamType } from "../models/team";

const router = useRouter();
const teamList = ref<TeamType[]>([]);
const searchText = ref("");
const active = ref<"public" | "secret">("public");
const loading = ref(false);

const loadTeams = async () => {
  loading.value = true;

  try {
    const res = await myAxios.get<TeamType[]>("/team/list", {
      params: {
        searchText: searchText.value.trim() || undefined,
        status: active.value === "public" ? 0 : 2,
      },
    });

    if (res.code === 0) {
      teamList.value = res.data || [];
    } else {
      teamList.value = [];
      Toast.fail(res.description || res.msg || "队伍加载失败");
    }
  } catch (error) {
    console.error("查询队伍失败：", error);
    teamList.value = [];
  } finally {
    loading.value = false;
  }
};

const onClear = () => {
  searchText.value = "";
  loadTeams();
};

const onTabChange = () => {
  searchText.value = "";
  loadTeams();
};

const doCreateTeam = () => {
  router.push("/team/add");
};

onMounted(loadTeams);
</script>

<style scoped>
.team-page {
  min-height: 100%;
  padding-bottom: 20px;
  background: #f6f8fc;
}

.team-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px 10px;
}

.header-text {
  flex: 1;
  min-width: 0;
}

.header-text h2 {
  margin: 0;
  color: #252a34;
  font-size: 22px;
  font-weight: 700;
}

.header-text p {
  margin: 5px 0 0;
  color: #969799;
  font-size: 12px;
  line-height: 1.5;
}

.create-button {
  flex-shrink: 0;
  height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: 14px;
  font-size: 14px;
  box-shadow: 0 5px 14px rgba(25, 137, 250, 0.18);
}

.search-wrapper {
  padding: 4px 16px 8px;
}

.team-search {
  padding: 0;
}

.team-search :deep(.van-search__content) {
  height: 42px;
  padding-left: 14px;
  background: #fff;
  border-radius: 14px;
}

.team-tabs {
  margin-bottom: 4px;
}

.team-tabs :deep(.van-tabs__wrap) {
  height: 46px;
}

.team-tabs :deep(.van-tabs__nav) {
  background: #fff;
}

.loading {
  padding: 50px 0;
  text-align: center;
}
</style>
