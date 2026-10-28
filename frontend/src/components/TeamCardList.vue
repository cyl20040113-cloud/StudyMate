<template>
  <div class="team-card-list">
    <div v-for="team in teamList" :key="team.id" class="team-card">
      <div class="team-header">
        <div class="team-title-wrap">
          <h3 class="team-name">{{ team.name }}</h3>
          <van-tag :type="team.status === 2 ? 'warning' : 'primary'" plain>
            {{ getTeamStatusText(team.status) }}
          </van-tag>
        </div>
        <div class="team-owner">
          队长：{{ team.createUser?.username || "未知用户" }}
        </div>
      </div>

      <div class="team-description">
        {{ team.description || "这个队伍暂时没有填写介绍" }}
      </div>

      <div class="team-meta">
        <span>👥 {{ team.hasJoinNum ?? 0 }}/{{ team.maxNum }} 人</span>
        <span
          >⏰
          {{
            team.expireTime ? formatDateTime(team.expireTime) : "长期有效"
          }}</span
        >
      </div>

      <div class="team-actions">
        <!-- 队长 -->
        <template v-if="currentUser && team.userId === currentUser.id">
          <van-button
            size="small"
            plain
            type="primary"
            @click="doUpdateTeam(team.id)"
          >
            编辑
          </van-button>
          <van-button
            size="small"
            plain
            type="warning"
            @click="doQuitTeam(team)"
          >
            退出队伍
          </van-button>
          <van-button
            size="small"
            plain
            type="danger"
            @click="doDeleteTeam(team)"
          >
            解散
          </van-button>
        </template>

        <!-- 非队长 -->
        <template v-else>
          <van-button
            v-if="team.hasJoin"
            size="small"
            plain
            type="danger"
            @click="doQuitTeam(team)"
          >
            退出队伍
          </van-button>

          <van-button
            v-else
            size="small"
            type="primary"
            @click="doJoinTeam(team)"
          >
            {{ team.status === 2 ? "输入密码加入" : "加入队伍" }}
          </van-button>
        </template>
      </div>
    </div>
  </div>

  <van-popup
    v-model:show="passwordDialogVisible"
    round
    position="center"
    :style="{ width: '82%', maxWidth: '340px' }"
  >
    <div class="password-popup">
      <div class="password-popup-title">加入加密队伍</div>
      <div class="password-popup-tip">请输入队伍密码后加入</div>

      <van-field
        v-model="teamPassword"
        type="password"
        placeholder="请输入队伍密码"
        maxlength="32"
        clearable
        autofocus
        class="password-field"
      />

      <div class="password-popup-actions">
        <van-button block plain @click="cancelJoinSecretTeam">取消</van-button>
        <van-button
          block
          type="primary"
          :loading="joiningSecretTeam"
          :disabled="joiningSecretTeam"
          @click="confirmJoinSecretTeam"
        >
          加入
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { Dialog, Toast } from "vant";
import myAxios from "../plugins/myAxios";
import type { TeamType } from "../models/team";
import type { UserDetailType } from "../models/user";
import { getCurrentUserState } from "../states/user";
import { getTeamStatusText } from "../utils/team";
import { formatDateTime } from "../utils/date";

interface Props {
  teamList: TeamType[];
}

withDefaults(defineProps<Props>(), {
  teamList: () => [],
});

const emit = defineEmits<{ refresh: [] }>();
const router = useRouter();
const currentUser = ref<UserDetailType | undefined>(getCurrentUserState());

const passwordDialogVisible = ref(false);
const teamPassword = ref("");
const selectedTeam = ref<TeamType | null>(null);
const joiningSecretTeam = ref(false);

const joinTeamRequest = async (teamId: number, password?: string) => {
  try {
    const res = await myAxios.post<boolean>("/team/join", { teamId, password });

    if (res.code === 0) {
      Toast.success("加入队伍成功");
      emit("refresh");
      return true;
    }

    Toast.fail(res.description || res.msg || "加入队伍失败");
  } catch (error) {
    console.error("加入队伍失败：", error);
    Toast.fail("加入队伍失败");
  }

  return false;
};

const doJoinTeam = async (team: TeamType) => {
  if (team.status === 2) {
    selectedTeam.value = team;
    teamPassword.value = "";
    passwordDialogVisible.value = true;
    return;
  }

  await joinTeamRequest(team.id);
};

const cancelJoinSecretTeam = () => {
  passwordDialogVisible.value = false;
  teamPassword.value = "";
  selectedTeam.value = null;
};

const confirmJoinSecretTeam = async () => {
  if (joiningSecretTeam.value || !selectedTeam.value) return;

  const password = teamPassword.value.trim();
  if (!password) {
    Toast("请输入队伍密码");
    return;
  }

  joiningSecretTeam.value = true;

  try {
    if (await joinTeamRequest(selectedTeam.value.id, password)) {
      cancelJoinSecretTeam();
    }
  } finally {
    joiningSecretTeam.value = false;
  }
};

const doQuitTeam = async (team: TeamType) => {
  const isCaptain = currentUser.value?.id === team.userId;

  let message = `确定退出「${team.name}」吗？`;

  if (isCaptain) {
    message =
      (team.hasJoinNum ?? 0) <= 1
        ? `当前只有你一名成员，退出后队伍将解散，确定退出吗？`
        : `退出后队长将自动转让给其他成员，确定退出吗？`;
  }

  try {
    await Dialog.confirm({
      title: isCaptain ? "队长退出队伍" : "退出队伍",
      message,
    });
  } catch {
    return;
  }

  try {
    const res = await myAxios.post<boolean>("/team/quit", {
      teamId: team.id,
    });

    if (res.code === 0) {
      Toast.success(
        isCaptain && (team.hasJoinNum ?? 0) > 1
          ? "已退出，队长已转让"
          : "已退出队伍",
      );
      emit("refresh");
    } else {
      Toast.fail(res.description || res.msg || "退出失败");
    }
  } catch (error) {
    console.error("退出队伍失败：", error);
    Toast.fail("退出失败");
  }
};

const doUpdateTeam = (teamId: number) => {
  router.push({
    path: "/team/update",
    query: { id: teamId },
  });
};

const doDeleteTeam = async (team: TeamType) => {
  try {
    await Dialog.confirm({
      title: "确认解散队伍",
      message: `确定要解散「${team.name}」吗？此操作不可撤销。`,
    });
  } catch {
    return;
  }

  try {
    const res = await myAxios.post<boolean>("/team/delete", {
      id: team.id,
    });

    if (res.code === 0) {
      Toast.success("队伍已解散");
      emit("refresh");
    } else {
      Toast.fail(res.description || res.msg || "解散失败");
    }
  } catch (error) {
    console.error("解散队伍失败：", error);
    Toast.fail("解散失败");
  }
};
</script>

<style scoped>
.team-card-list {
  padding: 12px 16px 20px;
}

.team-card {
  margin-bottom: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
}

.team-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.team-name {
  margin: 0;
  color: #252a34;
  font-size: 17px;
  font-weight: 600;
}

.team-owner {
  margin-top: 7px;
  color: #969799;
  font-size: 12px;
}

.team-description {
  margin-top: 14px;
  color: #646566;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.team-meta {
  margin-top: 14px;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  color: #8a94a3;
  font-size: 12px;
}

.team-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.password-popup {
  padding: 22px 18px 18px;
}

.password-popup-title {
  color: #252a34;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}

.password-popup-tip {
  margin-top: 8px;
  color: #969799;
  font-size: 13px;
  text-align: center;
}

.password-field {
  margin-top: 18px;
  background: #f7f8fa;
  border-radius: 10px;
}

.password-popup-actions {
  margin-top: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
</style>
