-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
-- documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
-- rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
-- permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of
-- the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
-- LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
-- NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
-- WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
-- SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-- 频率限制
if not accessControlUtil:isAccess() then
    ngx.log(ngx.ERR, "request excess!")
    ngx.exit(429)
    return
end

-- 安全限制
if not securityUtil:isSafe() then
    ngx.log(ngx.ERR, "unsafe request")
    ngx.exit(422)
end

-- 获取服务名称
local service_name = ngx.var.service
if config.service_name ~= nil and config.service_name ~= "" then
    service_name = config.service_name
end

if not service_name or service_name == "" then
    ngx.log(ngx.STDERR, "failed with service name :", tostring(service_name))
    ngx.exit(503)
    return
end

-- 当服务器为job的时候指向job的域名
if service_name == "job" then
    ngx.var.target = config.job.domain
    return
end

if service_name == "bkrepo" then
    ngx.var.target = config.bkrepo.domain
    return
end

-- 获取灰度设置
local cache_tail = ""
local devops_gray = grayUtil:get_gray()
local ns_config = nil
if devops_gray ~= true then
    if ngx.var.devops_region ~= "DEVNET" then
        ns_config = config.ns
        cache_tail = ".normal.idc"
    else
        ns_config = config.ns_devnet
        cache_tail = ".normal.devnet"
    end
else
    if ngx.var.devops_region ~= "DEVNET" then
        ns_config = config.ns_gray
        cache_tail = ".gray.idc"
    else
        ns_config = config.ns_devnet_gray
        cache_tail = ".gray.devnet"
    end
end
if not ns_config.ip then
    ngx.log(ngx.ERR, "DNS ip not exist!")
    ngx.exit(503)
    return
end

-- 获取tag设置
local devops_tag = tagUtil:get_tag(ns_config)
if devops_tag == nil then
    devops_tag = ns_config.tag
end

-- 负载均衡
local target = loadBalanceUtil:getTarget(devops_tag, service_name, cache_tail, ns_config)
if target == nil then
    -- 用默认tag
    if devops_tag ~= ns_config.tag then
        target = loadBalanceUtil:getTarget(ns_config.tag, service_name, cache_tail, ns_config)
    end

    if target == nil then
        ngx.exit(503)
    end
end

ngx.var.target = target
