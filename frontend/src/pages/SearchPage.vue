<template>
  <div class="search-page">
    <section class="search-header">
      <h2>按标签找伙伴</h2>
      <p>选择你的兴趣、方向和状态，找到更适合一起学习的伙伴</p>
    </section>

    <van-search
      v-model="searchText"
      placeholder="搜索标签，例如 Java、Redis"
      shape="round"
      clearable
    />

    <section class="selected-section">
      <div class="section-title">
        <span>已选标签</span>
        <span class="selected-count">{{ activeIds.length }}/5</span>
      </div>

      <div v-if="activeIds.length === 0" class="empty-selected">
        请选择至少一个标签
      </div>

      <div v-else class="selected-tags">
        <van-tag
          v-for="tag in activeIds"
          :key="tag"
          closeable
          size="medium"
          type="primary"
          @close="removeTag(tag)"
        >
          {{ tag }}
        </van-tag>
      </div>
    </section>

    <div class="section-title tag-title">
      <span>选择标签</span>
    </div>

    <van-tree-select
      v-model:active-id="activeIds"
      v-model:main-active-index="activeIndex"
      :items="filteredTagList"
      class="tag-tree"
    />

    <div class="search-button-wrapper">
      <van-button
        block
        round
        type="primary"
        :disabled="activeIds.length === 0"
        @click="doSearchResult"
      >
        搜索伙伴
        <span v-if="activeIds.length">（{{ activeIds.length }}）</span>
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { Toast } from "vant";

const router = useRouter();
const searchText = ref("");
const activeIndex = ref(0);
const activeIds = ref<string[]>([]);

const tagList = [
  {
    text: "学习方向",
    children: [
      { text: "Java", id: "Java" },
      { text: "Python", id: "Python" },
      { text: "C++", id: "C++" },
      { text: "Go", id: "Go" },
      { text: "前端", id: "前端" },
      { text: "Vue", id: "Vue" },
      { text: "Spring Boot", id: "Spring Boot" },
      { text: "Redis", id: "Redis" },
      { text: "MySQL", id: "MySQL" },
    ],
  },
  {
    text: "年级",
    children: [
      { text: "大一", id: "大一" },
      { text: "大二", id: "大二" },
      { text: "大三", id: "大三" },
      { text: "大四", id: "大四" },
      { text: "研究生", id: "研究生" },
    ],
  },
  {
    text: "学习目标",
    children: [
      { text: "找实习", id: "找实习" },
      { text: "找工作", id: "找工作" },
      { text: "考研", id: "考研" },
      { text: "刷题", id: "刷题" },
      { text: "做项目", id: "做项目" },
    ],
  },
  {
    text: "性别",
    children: [
      { text: "男", id: "男" },
      { text: "女", id: "女" },
    ],
  },
];

const filteredTagList = computed(() => {
  const keyword = searchText.value.trim().toLowerCase();
  if (!keyword) return tagList;

  return tagList
    .map((category) => ({
      ...category,
      children: category.children.filter((tag) =>
        tag.text.toLowerCase().includes(keyword),
      ),
    }))
    .filter((category) => category.children.length > 0);
});

watch(
  activeIds,
  (newTags, oldTags) => {
    if (newTags.length <= 5) return;
    activeIds.value = oldTags ? [...oldTags] : newTags.slice(0, 5);
    Toast("最多只能选择 5 个标签");
  },
  { deep: true },
);

const removeTag = (tag: string) => {
  activeIds.value = activeIds.value.filter((item) => item !== tag);
};

const doSearchResult = () => {
  if (!activeIds.value.length) {
    Toast("请至少选择一个标签");
    return;
  }

  router.push({
    path: "/user/list",
    query: { tags: activeIds.value },
  });
};
</script>

<style scoped>
.search-page {
  min-height: 100%;
  padding-bottom: 90px;
  background: #f6f8fc;
}

.search-header {
  padding: 20px 16px 10px;
}

.search-header h2 {
  margin: 0;
  color: #252a34;
  font-size: 22px;
}

.search-header p {
  margin: 8px 0 0;
  color: #969799;
  font-size: 13px;
  line-height: 1.6;
}

.selected-section {
  margin: 12px 16px;
  padding: 16px;
  background: #fff;
  border-radius: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #323233;
  font-size: 15px;
  font-weight: 600;
}

.selected-count {
  color: #969799;
  font-size: 12px;
  font-weight: normal;
}

.empty-selected {
  margin-top: 14px;
  color: #c8c9cc;
  font-size: 13px;
}

.selected-tags {
  margin-top: 13px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-title {
  padding: 10px 16px;
}

.tag-tree {
  margin: 0 16px;
  overflow: hidden;
  border-radius: 16px;
}

.search-button-wrapper {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 50px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
}
</style>
