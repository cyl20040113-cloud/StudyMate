<template>
  <div class="team-add-page">
    <section class="page-header">
      <h2>创建学习队伍</h2>
      <p>创建一个属于你的学习小组，邀请伙伴一起进步</p>
    </section>

    <van-form @submit="onSubmit">
      <div class="form-card">
        <div class="section-title">基本信息</div>

        <van-field
          v-model="form.name"
          label="队伍名称"
          placeholder="例如：Java 后端学习小组"
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
          placeholder="简单介绍一下这个队伍的学习目标吧"
        />

        <van-field label="最大人数">
          <template #input>
            <van-stepper v-model="form.maxNum" :min="1" :max="20" integer />
          </template>
        </van-field>
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
            私有队伍不会出现在普通公开搜索中。
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
          placeholder="请输入加入密码"
          maxlength="32"
          clearable
          :rules="[{ required: true, message: '加密队伍必须设置密码' }]"
        />

        <van-field
          :model-value="expireTimeText"
          readonly
          clickable
          label="过期时间"
          placeholder="请选择队伍过期时间"
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
          loading-text="正在创建..."
        >
          创建队伍
        </van-button>
      </div>
    </van-form>

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
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { Toast } from "vant";

import myAxios from "../plugins/myAxios";
import { formatDateTime } from "../utils/date";

interface TeamAddForm {
  name: string;
  description: string;
  maxNum: number;
  expireTime: Date | null;
  status: 0 | 1 | 2;
  password: string;
}

const router = useRouter();

const form = ref<TeamAddForm>({
  name: "",
  description: "",
  maxNum: 5,
  expireTime: null,
  status: 0,
  password: "",
});

const showDatePicker = ref(false);
const submitting = ref(false);
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

const confirmDate = (value: Date) => {
  form.value.expireTime = value;
  currentDate.value = value;
  showDatePicker.value = false;
};

const onSubmit = async () => {
  if (submitting.value) return;

  const name = form.value.name.trim();
  const description = form.value.description.trim();
  const password = form.value.password.trim();

  if (!name) return Toast("请输入队伍名称");
  if (name.length > 20) return Toast("队伍名称不能超过 20 个字符");
  if (description.length > 512) return Toast("队伍描述不能超过 512 个字符");
  if (form.value.maxNum < 1 || form.value.maxNum > 20) {
    return Toast("队伍人数必须在 1 到 20 人之间");
  }
  if (!form.value.expireTime) return Toast("请选择过期时间");
  if (form.value.expireTime.getTime() <= Date.now()) {
    return Toast("过期时间必须晚于当前时间");
  }
  if (form.value.status === 2 && !password) {
    return Toast("加密队伍必须设置密码");
  }
  if (password.length > 32) return Toast("队伍密码不能超过 32 个字符");

  submitting.value = true;

  try {
    const res = await myAxios.post<number>("/team/add", {
      name,
      description,
      maxNum: form.value.maxNum,
      expireTime: form.value.expireTime.toISOString(),
      status: form.value.status,
      password: form.value.status === 2 ? password : undefined,
    });

    if (res.code === 0) {
      Toast.success("创建队伍成功");
      router.replace("/team");
    } else {
      Toast.fail(res.description || res.msg || "创建队伍失败");
    }
  } catch (error) {
    console.error("创建队伍失败：", error);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.team-add-page {
  min-height: 100%;
  padding-bottom: 30px;
  background: #f6f8fc;
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

.submit-wrapper {
  padding: 8px 20px 20px;
}
</style>
