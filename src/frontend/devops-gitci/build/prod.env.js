/**
 * @file production env
 * @author Blueking
 */

const NODE_ENV = JSON.stringify('production')

export default {
    'process.env': {
        'NODE_ENV': NODE_ENV
    },
    NODE_ENV: NODE_ENV,
    LOGIN_URL: JSON.stringify(''),
    AJAX_URL_PREFIX: JSON.stringify('http://dev.devops.oa.com'),
    AJAX_MOCK_PARAM: JSON.stringify(''),
    USER_INFO_URL: JSON.stringify('')
}
