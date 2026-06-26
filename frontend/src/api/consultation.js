import request from './request'

function unwrapResult(response) {
  const result = response.data

  if (result?.code !== 200) {
    throw new Error(result?.message || '请求失败，请稍后重试')
  }

  return result.data
}

export async function createConsultation(payload) {
  return unwrapResult(await request.post('/patient/consultation', payload))
}

export async function getMyConsultations(params) {
  return unwrapResult(await request.get('/patient/consultation/my', { params }))
}

export async function getConsultationMessages(id) {
  return unwrapResult(await request.get(`/patient/consultation/${id}/messages`))
}

export async function sendConsultationMessage(id, content) {
  return unwrapResult(await request.post(`/patient/consultation/${id}/messages`, { content }))
}
