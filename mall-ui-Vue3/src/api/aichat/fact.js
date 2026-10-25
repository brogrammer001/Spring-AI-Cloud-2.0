import request from '@/utils/request'

// 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)列表
export function listFact(query) {
  return request({
    url: '/ai-chat/fact/list',
    method: 'get',
    params: query
  })
}

// 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)详细
export function getFact(id) {
  return request({
    url: '/ai-chat/fact/' + id,
    method: 'get'
  })
}

// 新增AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
export function addFact(data) {
  return request({
    url: '/ai-chat/fact',
    method: 'post',
    data: data
  })
}

// 修改AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
export function updateFact(data) {
  return request({
    url: '/ai-chat/fact',
    method: 'put',
    data: data
  })
}

// 删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
export function delFact(id) {
  return request({
    url: '/ai-chat/fact/' + id,
    method: 'delete'
  })
}

// 同步选中的长期记忆到向量库：先删除旧向量，再按 MySQL 数据重建索引
export function syncFactVector(ids) {
  return request({
    url: '/ai-chat/fact/syncVector',
    method: 'post',
    data: ids
  })
}
