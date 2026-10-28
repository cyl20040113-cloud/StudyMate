<template>
  <div class="login-page">
    <section class="brand-section">
      <div class="brand-logo">S</div>
      <h1>StudyMate</h1>
      <div class="brand-cn">伴学星球</div>
      <p>找到志同道合的学习伙伴</p>
    </section>

    <section class="login-card">
      <div class="card-title">欢迎回来</div>
      <div class="card-description">登录后继续寻找你的学习伙伴</div>

      <van-form @submit="onSubmit">
        <van-field
          v-model="form.userAccount"
          label="账号"
          placeholder="请输入账号"
          clearable
          autocomplete="username"
          :rules="[{ required: true, message: '请输入账号' }]"
        />

        <van-field
          v-model="form.userPassword"
          label="密码"
          type="password"
          placeholder="请输入密码"
          clearable
          autocomplete="current-password"
          :rules="[{ required: true, message: '请输入密码' }]"
        />

        <div class="submit-wrapper">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="submitting"
            :disabled="submitting"
            loading-text="正在登录..."
          >
            登录
          </van-button>
        </div>
      </van-form>

      <div class="register-entry">
        <span>还没有账号？</span>
        <span class="register-link" @click="router.push('/user/register')">
          立即注册
        </span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Toast } from "vant";

import myAxios from "../plugins/myAxios";
import { getCurrentUser } from "../services/user";

interface LoginForm {
  userAccount: string;
  userPassword: string;
}

const route = useRoute();
const router = useRouter();

const form = ref<LoginForm>({
  userAccount: "",
  userPassword: "",
});

const submitting = ref(false);

const onSubmit = async () => {
  if (submitting.value) return;

  const userAccount = form.value.userAccount.trim();
  const userPassword = form.value.userPassword;

  if (!userAccount) return Toast("请输入账号");
  if (!userPassword) return Toast("请输入密码");
  if (userAccount.length < 4) return Toast("账号长度不能少于 4 位");
  if (userPassword.length < 8) return Toast("密码长度不能少于 8 位");

  submitting.value = true;

  try {
    const res = await myAxios.post<unknown>("/user/login", {
      userAccount,
      userPassword,
    });

    if (res.code !== 0) {
      Toast.fail(res.description || res.msg || "登录失败");
      return;
    }

    const currentUser = await getCurrentUser();

    if (!currentUser) {
      Toast.fail("登录状态获取失败");
      return;
    }

    Toast.success("登录成功");

    const redirect = route.query.redirect;
    const redirectUrl = route.query.redirectUrl;

    if (typeof redirect === "string" && redirect.startsWith("/")) {
      router.replace(redirect);
    } else if (typeof redirectUrl === "string" && redirectUrl.startsWith("/")) {
      router.replace(redirectUrl);
    } else {
      router.replace("/");
    }
  } catch (error) {
    console.error("登录失败：", error);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 50px 20px 30px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #eef1ff 0%, #f7f8fc 42%, #f7f8fc 100%);
}

.brand-section {
  text-align: center;
}

.brand-logo {
  width: 62px;
  height: 62px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 30px;
  font-weight: 700;
  background: linear-gradient(135deg, #6676f6, #8c74f7);
  border-radius: 20px;
  box-shadow: 0 12px 28px rgba(103, 118, 246, 0.25);
}

.brand-section h1 {
  margin: 16px 0 0;
  color: #252a34;
  font-size: 27px;
}

.brand-cn {
  margin-top: 4px;
  color: #6f7bf7;
  font-size: 15px;
  font-weight: 600;
}

.brand-section p {
  margin: 9px 0 0;
  color: #969799;
  font-size: 13px;
}

.login-card {
  margin-top: 34px;
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

.login-card :deep(.van-field) {
  margin-bottom: 12px;
  padding: 13px 14px;
  background: #f7f8fa;
  border-radius: 12px;
}

.submit-wrapper {
  margin-top: 22px;
}

.register-entry {
  margin-top: 20px;
  color: #969799;
  font-size: 13px;
  text-align: center;
}

.register-link {
  margin-left: 5px;
  color: #6f7bf7;
  font-weight: 600;
  cursor: pointer;
}
</style>
