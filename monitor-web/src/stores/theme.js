import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
    // 默认从本地读取，如果没有则默认 false (亮色)
    const isDark = ref(localStorage.getItem('isDark') === 'true')

    const toggleDark = () => {
        isDark.value = !isDark.value
        applyTheme()
        localStorage.setItem('isDark', isDark.value)
    }

    const applyTheme = () => {
        const html = document.documentElement
        if (isDark.value) {
            html.classList.add('dark')
        } else {
            html.classList.remove('dark')
        }
    }

    // 初始化时应用一次
    applyTheme()

    return { isDark, toggleDark }
})