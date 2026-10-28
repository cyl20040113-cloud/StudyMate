<template>
  <div class="register-page">
    <section class="brand-section">
      <div class="brand-logo">S</div>
      <h1>StudyMate</h1>
      <div class="brand-cn">伴学星球</div>
      <p>创建账号，开始寻找你的学习伙伴</p>
    </section>

    <section class="register-card">
      <div class="card-title">创建账号</div>
      <div class="card-description">填写基本信息完成注册</div>

      <van-form @submit="onSubmit">
        <van-field
          v-model="form.userAccount"
          label="账号"
          placeholder="请输入账号"
          maxlength="20"
          clearable
          autocomplete="username"
          :rules="[{ required: true, message: '请输入账号' }]"
        />

        <van-field
          v-model="form.planetCode"
          label="星球编号"
          placeholder="请输入星球编号"
          maxlength="12"
          clearable
        />

        <van-field
          v-model="form.userPassword"
          label="密码"
          type="password"
          placeholder="请输入至少 8 位密码"
          maxlength="30"
          clearable
          autocomplete="new-password"
          :rules="[{ required: true, message: '请输入密码' }]"
        />

        <van-field
          v-model="form.checkPassword"
          label="确认密码"
          type="password"
          placeholder="请再次输入密码"
          maxlength="30"
          clearable
          autocomplete="new-password"
          :rules="[{ required: true, message: '请再次输入密码' }]"
        />

        <div class="submit-wrapper">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="submitting"
            :disabled="submitting"
            loading-text="正在注册..."
          >
            注册
          </van-button>
        </div>
      </van-form>

      <div class="login-entry">
        <span>已经有账号？</span>
        <span class="login-link" @click="router.replace('/user/login')">
          返回登录
        </span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { Toast } from "vant";

import myAxios from "../plugins/myAxios";

interface RegisterForm {
  userAccount: string;
  planetCode: string;
  userPassword: string;
  checkPassword: string;
}

const router = useRouter();

const form = ref<RegisterForm>({
  userAccount: "",
  planetCode: "",
  userPassword: "",
  checkPassword: "",
});

const submitting = ref(false);

const onSubmit = async () => {
  if (submitting.value) return;

  const userAccount = form.value.userAccount.trim();
  const planetCode = form.value.planetCode.trim();
  const userPassword = form.value.userPassword;
  const checkPassword = form.value.checkPassword;

  if (!userAccount) return Toast("请输入账号");
  if (userAccount.length < 4) return Toast("账号长度不能少于 4 位");
  if (!/^[a-zA-Z0-9_.]+$/.test(userAccount)) {
    return Toast("账号只能包含字母、数字、下划线和点");
  }

  if (!planetCode) return Toast("请输入星球编号");
  if (userPassword.length < 8) return Toast("密码长度不能少于 8 位");
  if (userPassword !== checkPassword) return Toast("两次输入的密码不一致");

  submitting.value = true;

  try {
    const res = await myAxios.post<number>("/user/register", {
      userAccount,
      planetCode,
      userPassword,
      checkPassword,
    });

    if (res.code === 0) {
      Toast.success("注册成功，请登录");
      router.replace({
        path: "/user/login",
        query: { account: userAccount },
      });
    } else {
      Toast.fail(res.description || res.msg || "注册失败");
    }
  } catch (error) {
    console.error("注册失败：", error);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  padding: 40px 20px 30px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #eef1ff 0%, #f7f8fc 42%, #f7f8fc 100%);
}

.brand-section {
  text-align: center;
}

.brand-logo {
  width: 58px;
  height: 58px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #6676f6, #8c74f7);
  border-radius: 18px;
  box-shadow: 0 12px 28px rgba(103, 118, 246, 0.25);
}

.brand-section h1 {
  margin: 14px 0 0;
  color: #252a34;
  font-size: 26px;
}

.brand-cn {
  margin-top: 4px;
  color: #6f7bf7;
  font-size: 15px;
  font-weight: 600;
}

.brand-section p {
  margin: 8px 0 0;
  color: #969799;
  font-size: 13px;
}

.register-card {
  margin-top: 28px;
  padding: 24px 18px;
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.05);
}

.card-title {
  color: #252a34;
  font-size: 21px;
  font-weight: 600;
}

.card-description {
  margin: 6px 0 20px;
  color: #969799;
  font-size: 13px;
}

.register-card :deep(.van-field) {
  margin-bottom: 12px;
  padding: 13px 14px;
  background: #f7f8fa;
  border-radius: 12px;
}

.submit-wrapper {
  margin-top: 22px;
}

.login-entry {
  margin-top: 20px;
  color: #969799;
  font-size: 13px;
  text-align: center;
}

.login-link {
  margin-left: 5px;
  color: #6f7bf7;
  font-weight: 600;
  cursor: pointer;
}
</style>
