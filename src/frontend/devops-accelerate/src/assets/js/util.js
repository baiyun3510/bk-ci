import { bkMessage } from 'bk-magic-vue'

module.exports = {
    copy (value) {
        const input = document.createElement('input')
        document.body.appendChild(input)
        input.setAttribute('value', value)
        input.select()
        if (document.execCommand('copy')) {
            document.execCommand('copy')
            bkMessage({ theme: 'success', message: '已成功复制到粘贴板' })
        }
        document.body.removeChild(input)
    }
}
