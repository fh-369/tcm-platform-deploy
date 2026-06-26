export function getApiErrorMessage(error, fallback = '操作失败，请稍后重试') {
  const businessMessage = error?.response?.data?.message
  if (businessMessage) return businessMessage

  if (error?.code === 'ERR_NETWORK' || error?.message === 'Network Error') {
    return '无法连接服务器，请检查后端服务和网络连接'
  }

  if (error?.code === 'ECONNABORTED') {
    return '请求超时，请稍后重试'
  }

  return error?.message || fallback
}
