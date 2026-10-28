<template>
  <div class="team-page">
    <div class="team-toolbar">
      <van-search
        v-model="searchText"
        placeholder="搜索我创建的队伍"
        shape="round"
        clearable
        class="team-search"
        @search="listTeam"
        @clear="onClear"
      />

      <van-button
        round
        type="primary"
        icon="plus"
        class="create-button"
        @click="doCreateTeam"
      >
        新建
      </van-button>
    </div>

    <div v-if="loading" class="loading">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <TeamCardList
      v-else
      :team-list="teamList"
      @refresh="listTeam(searchText)"
    />

    <van-empty
      v-if="!loading && !teamList.length"
      description="暂无创建的队伍"
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
const searchText = ref("");
const teamList = ref<TeamType[]>([]);
const loading = ref(false);

const doCreateTeam = () => {
  router.push("/team/add");
};

const listTeam = async (search = "") => {
  loading.value = true;

  try {
    const res = await myAxios.get<TeamType[]>("/team/list/my/create", {
      params: {
        searchText: search.trim() || undefined,
        pageNum: 1,
      },
    });

    if (res.code === 0) {
      teamList.value = res.data || [];
    } else {
      teamList.value = [];
      Toast.fail(res.description || res.msg || "加载队伍失败");
    }
  } catch (error) {
    console.error("加载创建队伍失败：", error);
    teamList.value = [];
  } finally {
    loading.value = false;
  }
};

const onClear = () => {
  searchText.value = "";
  listTeam();
};

onMounted(() => listTeam());
</script>

<style scoped>
.team-page {
  min-height: calc(100vh - 96px);
  padding: 12px 16px 20px;
  box-sizing: border-box;
  background: #f6f8fc;
}

.team-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.team-search {
  flex: 1;
  min-width: 0;
  padding: 0;
}

.team-search :deep(.van-search__content) {
  height: 42px;
  padding-left: 14px;
  background: #fff;
  border-radius: 14px;
}

.create-button {
  flex-shrink: 0;
  height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 5px 14px rgba(25, 137, 250, 0.2);
}

.loading {
  padding: 50px 0;
  text-align: center;
}
</style>
