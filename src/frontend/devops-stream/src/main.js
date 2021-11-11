/**
 * @file main entry
 * @author <%- author %>
 */

import '../build/public-path'
import Vue from 'vue'

import App from '@/App'
import router from '@/router'
import store from '@/store'
import '@/css/index.css'
import '@/common/bkmagic'
import '@icon-cool/bk-icon-stream/src/index'
import '@icon-cool/bk-icon-stream'
import icon from '@/components/icon'
import log from '@blueking/log'
import VeeValidate from 'vee-validate'
import VueCompositionAPI from '@vue/composition-api'
import { bkMessage } from 'bk-magic-vue'

Vue.component('Icon', icon)
Vue.use(log)
Vue.use(VeeValidate)
Vue.use(VueCompositionAPI)

Vue.prototype.$bkMessage = function (config) {
    config.ellipsisLine = config.ellipsisLine || 3
    bkMessage(config)
}

window.mainComponent = new Vue({
    el: '#app',
    router,
    store,
    components: { App },
    template: '<App/>'
})
