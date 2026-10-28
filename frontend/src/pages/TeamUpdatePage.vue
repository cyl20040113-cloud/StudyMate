<template>
  <div class="team-update-page">
    <div v-if="loading" class="loading-wrapper">
      <van-loading size="24" vertical>正在加载队伍信息...</van-loading>
    </div>

    <template v-else>
      <section class="page-header">
        <h2>编辑队伍</h2>
        <p>修改队伍的名称、介绍、类型和有效时间</p>
      </section>

      <van-form @submit="onSubmit">
        <div class="form-card">
          <div class="section-title">基本信息</div>

          <van-field
            v-model="form.name"
            label="队伍名称"
            placeholder="请输入队伍名称"
            maxlength="20"
            show-word-limit
            clearable
            :rules="[{ required: true, message: '请输入队伍名称' }]"
          />

          <van-field
            v-model="form.description"
            label="队伍描述"
            type="textarea"
            rows="3"
            autosize
            maxlength="512"
            show-word-limit
            placeholder="请输入队伍介绍"
          />

          <van-field
            :model-value="maxNum ? `${maxNum} 人` : '未知'"
            label="最大人数"
            readonly
          />
        </div>

        <div class="form-card">
          <div class="section-title">队伍设置</div>

          <van-field label="队伍类型">
            <template #input>
              <van-radio-group v-model="form.status" direction="horizontal">
                <van-radio :name="0">公开</van-radio>
                <van-radio :name="1">私有</van-radio>
                <van-radio :name="2">加密</van-radio>
              </van-radio-group>
            </template>
          </van-field>

          <div class="status-tip">
            <template v-if="form.status === 0">
              公开队伍可以被其他用户搜索并直接加入。
            </template>
            <template v-else-if="form.status === 1">
              私有队伍不会出现在普通队伍搜索中。
            </template>
            <template v-else>
              加密队伍可以被搜索，但加入时必须输入正确密码。
            </template>
          </div>

          <van-field
            v-if="form.status === 2"
            v-model="form.password"
            type="password"
            label="队伍密码"
            placeholder="请输入新的队伍密码"
            maxlength="32"
            clearable
          />

          <div v-if="form.status === 2" class="password-tip">
            编辑加密队伍时，请重新输入队伍密码。
          </div>

          <van-field
            :model-value="expireTimeText"
            label="过期时间"
            readonly
            clickable
            placeholder="请选择过期时间"
            @click="showDatePicker = true"
          />
        </div>

        <div class="submit-wrapper">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="submitting"
            :disabled="submitting"
            loading-text="正在保存..."
          >
            保存修改
          </van-button>
        </div>
      </van-form>
    </template>

    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-datetime-picker
        v-model="currentDate"
        type="datetime"
        title="选择过期时间"
        :min-date="minDate"
        @confirm="confirmDate"
        @cancel="showDatePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Toast } from "vant";

import myAxios from "../plugins/myAxios";
import { getCurrentUserState } from "../states/user";
import { formatDateTime } from "../utils/date";
import type { TeamType } from "../models/team";

interface TeamUpdateForm {
  name: string;
  description: string;
  expireTime: Date | null;
  status: 0 | 1 | 2;
  password: string;
}

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const showDatePicker = ref(false);

const teamId = ref<number | null>(null);
const maxNum = ref<number | null>(null);

const form = ref<TeamUpdateForm>({
  name: "",
  description: "",
  expireTime: null,
  status: 0,
  password: "",
});

const minDate = new Date();
const currentDate = ref(new Date(Date.now() + 24 * 60 * 60 * 1000));

const expireTimeText = computed(() =>
  form.value.expireTime ? formatDateTime(form.value.expireTime) : "",
);

watch(
  () => form.value.status,
  (status) => {
    if (status !== 2) form.value.password = "";
  },
);

const loadTeam = async () => {
  if (!teamId.value) {
    Toast.fail("队伍参数错误");
    return router.replace("/team");
  }

  loading.value = true;

  try {
    const res = await myAxios.get<TeamType>("/team/get", {
      params: { id: teamId.value },
    });

    if (res.code !== 0) {
      Toast.fail(res.description || res.msg || "获取队伍信息失败");
      return router.replace("/team");
    }

    const team = res.data;
    if (!team) {
      Toast.fail("队伍不存在");
      return router.replace("/team");
    }

    const currentUser = getCurrentUserState();

    if (currentUser && team.userId !== currentUser.id) {
      Toast.fail("只有队长可以编辑队伍");
      return router.replace("/team");
    }

    form.value.name = team.name || "";
    form.value.description = team.description || "";
    form.value.status = team.status;
    form.value.password = "";
    maxNum.value = team.maxNum;

    if (team.expireTime) {
      const date = new Date(team.expireTime);

      if (!Number.isNaN(date.getTime())) {
        form.value.expireTime = date;
        currentDate.value = date;
      }
    }
  } catch (error) {
    console.error("获取队伍信息失败：", error);
  } finally {
    loading.value = false;
  }
};

const confirmDate = (value: Date) => {
  form.value.expireTime = value;
  currentDate.value = value;
  showDatePicker.value = false;
};

const onSubmit = async () => {
  if (submitting.value) return;
  if (!teamId.value) return Toast.fail("队伍参数错误");

  const name = form.value.name.trim();
  const description = form.value.description.trim();
  const password = form.value.password.trim();

  if (!name) return Toast("请输入队伍名称");
  if (name.length > 20) return Toast("队伍名称不能超过 20 个字符");
  if (description.length > 512) return Toast("队伍描述不能超过 512 个字符");
  if (!form.value.expireTime) return Toast("请选择过期时间");

  if (form.value.expireTime.getTime() <= Date.now()) {
    return Toast("过期时间必须晚于当前时间");
  }

  if (form.value.status === 2 && !password) {
    return Toast("加密队伍请重新输入密码");
  }

  if (password.length > 32) {
    return Toast("密码不能超过 32 个字符");
  }

  submitting.value = true;

  try {
    const res = await myAxios.post<boolean>("/team/update", {
      id: teamId.value,
      name,
      description,
      expireTime: form.value.expireTime.toISOString(),
      status: form.value.status,
      password: form.value.status === 2 ? password : undefined,
    });

    if (res.code === 0) {
      Toast.success("队伍修改成功");
      router.replace("/team");
    } else {
      Toast.fail(res.description || res.msg || "修改失败");
    }
  } catch (error) {
    console.error("修改队伍失败：", error);
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  const id = Number(route.query.id);

  if (!Number.isInteger(id) || id <= 0) {
    Toast.fail("队伍 id 不正确");
    router.replace("/team");
    return;
  }

  teamId.value = id;
  loadTeam();
});
</script>

<style scoped>
.team-update-page {
  min-height: 100%;
  padding-bottom: 30px;
  background: #f6f8fc;
}

.loading-wrapper {
  padding: 80px 0;
}

.page-header {
  padding: 20px 16px 12px;
}

.page-header h2 {
  margin: 0;
  color: #252a34;
  font-size: 22px;
}

.page-header p {
  margin: 7px 0 0;
  color: #969799;
  font-size: 13px;
  line-height: 1.6;
}

.form-card {
  margin: 0 16px 14px;
  overflow: hidden;
  background: #fff;
  border-radius: 18px;
}

.section-title {
  padding: 15px 16px 6px;
  color: #323233;
  font-size: 15px;
  font-weight: 600;
}

.status-tip {
  margin: 0 16px 10px;
  padding: 10px 12px;
  color: #7d8597;
  font-size: 12px;
  line-height: 1.6;
  background: #f7f8fc;
  border-radius: 10px;
}

.password-tip {
  margin: 0 16px 12px;
  color: #969799;
  font-size: 12px;
  line-height: 1.6;
}

.submit-wrapper {
  padding: 8px 20px 20px;
}
</style>
