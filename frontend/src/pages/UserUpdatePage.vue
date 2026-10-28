<template>
  <div class="user-update-page">
    <div v-if="loading" class="loading-wrapper">
      <van-loading vertical size="24">正在加载个人资料...</van-loading>
    </div>

    <template v-else-if="userId">
      <section class="page-header">
        <h2>编辑个人资料</h2>
        <p>完善你的个人信息和学习标签，让其他伙伴更容易了解你</p>
      </section>

      <van-form @submit="onSubmit">
        <section class="form-card">
          <div class="section-title">基本资料</div>

          <div class="avatar-section">
            <van-image round width="76" height="76" :src="form.avatarUrl">
              <template #error>
                <div class="avatar-placeholder">
                  {{ getAvatarText(form.username) }}
                </div>
              </template>
              <template #loading>
                <van-loading size="20" />
              </template>
            </van-image>
          </div>

          <van-field
            v-model="form.avatarUrl"
            label="头像地址"
            placeholder="请输入头像图片 URL"
            clearable
          />

          <van-field
            v-model="form.username"
            label="昵称"
            placeholder="请输入昵称"
            maxlength="20"
            clearable
            :rules="[{ required: true, message: '请输入昵称' }]"
          />

          <van-field label="性别">
            <template #input>
              <van-radio-group v-model="form.gender" direction="horizontal">
                <van-radio :name="0">男</van-radio>
                <van-radio :name="1">女</van-radio>
              </van-radio-group>
            </template>
          </van-field>
        </section>

        <section class="form-card">
          <div class="section-title">个人介绍</div>

          <van-field
            v-model="form.profile"
            label="个人简介"
            type="textarea"
            rows="3"
            autosize
            maxlength="512"
            show-word-limit
            placeholder="介绍一下自己的学习方向、目标或兴趣吧"
          />
        </section>

        <section class="form-card">
          <div class="section-title">我的标签</div>

          <div v-if="tags.length" class="tag-list">
            <van-tag
              v-for="tag in tags"
              :key="tag"
              closeable
              type="primary"
              size="medium"
              @close="removeTag(tag)"
            >
              {{ tag }}
            </van-tag>
          </div>

          <div v-else class="empty-tags">暂时还没有标签</div>

          <div class="tag-input-row">
            <van-field
              v-model="newTag"
              placeholder="例如：Java"
              maxlength="20"
              clearable
              @keyup.enter="addTag"
            />
            <van-button size="small" type="primary" plain @click="addTag">
              添加
            </van-button>
          </div>

          <div class="tag-tip">
            最多添加 10 个标签，例如 Java、Redis、大二、找实习
          </div>
        </section>

        <section class="form-card">
          <div class="section-title">联系信息</div>

          <van-field
            v-model="form.phone"
            label="手机号"
            type="tel"
            placeholder="请输入手机号"
            maxlength="20"
            clearable
          />

          <van-field
            v-model="form.email"
            label="邮箱"
            type="email"
            placeholder="请输入邮箱"
            clearable
          />
        </section>

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
            保存资料
          </van-button>
        </div>
      </van-form>
    </template>

    <van-empty v-else description="用户信息加载失败">
      <van-button round type="primary" @click="loadUser">重新加载</van-button>
    </van-empty>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Toast } from "vant";

import myAxios from "../plugins/myAxios";
import { getCurrentUser } from "../services/user";
import type { UserDetailType } from "../models/user";

interface UserUpdateForm {
  username: string;
  avatarUrl: string;
  gender: number;
  phone: string;
  email: string;
  profile: string;
}

const router = useRouter();
const userId = ref<number | null>(null);
const loading = ref(false);
const submitting = ref(false);
const tags = ref<string[]>([]);
const newTag = ref("");

const form = ref<UserUpdateForm>({
  username: "",
  avatarUrl: "",
  gender: 0,
  phone: "",
  email: "",
  profile: "",
});

const loadUser = async () => {
  loading.value = true;

  try {
    const user: UserDetailType | null = await getCurrentUser();

    if (!user) {
      userId.value = null;
      Toast.fail("获取用户信息失败");
      return;
    }

    userId.value = user.id;

    form.value = {
      username: user.username || "",
      avatarUrl: user.avatarUrl || "",
      gender: user.gender ?? 0,
      phone: user.phone || "",
      email: user.email || "",
      profile: user.profile || "",
    };

    tags.value = user.tags ? [...user.tags] : [];
  } catch (error) {
    console.error("加载用户资料失败：", error);
    userId.value = null;
  } finally {
    loading.value = false;
  }
};

const addTag = () => {
  const tag = newTag.value.trim();

  if (!tag) return;
  if (tags.value.length >= 10) return Toast("最多添加 10 个标签");
  if (tags.value.includes(tag)) return Toast("该标签已经存在");
  if (tag.length > 20) return Toast("单个标签不能超过 20 个字符");

  tags.value.push(tag);
  newTag.value = "";
};

const removeTag = (tag: string) => {
  tags.value = tags.value.filter((item) => item !== tag);
};

const onSubmit = async () => {
  if (submitting.value) return;
  if (!userId.value) return Toast.fail("用户信息不存在");

  const username = form.value.username.trim();
  const email = form.value.email.trim();

  if (!username) return Toast("请输入昵称");
  if (username.length > 20) return Toast("昵称不能超过 20 个字符");
  if (form.value.profile.length > 512) {
    return Toast("个人简介不能超过 512 个字符");
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return Toast("邮箱格式不正确");
  }

  submitting.value = true;

  try {
    const res = await myAxios.post<number>("/user/update", {
      id: userId.value,
      username,
      avatarUrl: form.value.avatarUrl.trim(),
      gender: form.value.gender,
      phone: form.value.phone.trim(),
      email,
      profile: form.value.profile.trim(),
      tags: JSON.stringify(tags.value),
    });

    if (res.code === 0) {
      await getCurrentUser();
      Toast.success("资料保存成功");
      router.replace("/user");
    } else {
      Toast.fail(res.description || res.msg || "保存失败");
    }
  } catch (error) {
    console.error("修改个人资料失败：", error);
  } finally {
    submitting.value = false;
  }
};

const getAvatarText = (username?: string) => username?.charAt(0) || "伴";

onMounted(loadUser);
</script>

<style scoped>
.user-update-page {
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

.avatar-section {
  padding: 12px 16px 18px;
  display: flex;
  justify-content: center;
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

.tag-list {
  padding: 12px 16px 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-tags {
  padding: 12px 16px 4px;
  color: #c8c9cc;
  font-size: 13px;
}

.tag-input-row {
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag-input-row :deep(.van-field) {
  flex: 1;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 10px;
}

.tag-tip {
  padding: 0 16px 14px;
  color: #969799;
  font-size: 12px;
}

.submit-wrapper {
  padding: 8px 20px 20px;
}
</style>
