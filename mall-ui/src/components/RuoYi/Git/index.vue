<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'mallGit',
  data() {
    return {
      url: 'https://gitee.com/y_project/mall-Cloud'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
