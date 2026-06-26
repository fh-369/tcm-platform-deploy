import request from './request'

function unwrapResult(response) {
  const result = response.data

  if (result?.code !== 200) {
    throw new Error(result?.message || '请求失败，请稍后重试')
  }

  return result.data
}

export async function getDepartmentPool(params) {
  return unwrapResult(await request.get('/doctor/consultations/pool', { params }))
}

export async function getMyDoctorConsultations(params) {
  return unwrapResult(await request.get('/doctor/consultations/mine', { params }))
}

export async function claimDoctorConsultation(id) {
  return unwrapResult(await request.put(`/doctor/consultations/${id}/claim`))
}

export async function updateDoctorConsultation(id, payload) {
  return unwrapResult(await request.put(`/doctor/consultations/${id}`, payload))
}

export async function getDoctorConsultationMessages(id) {
  return unwrapResult(await request.get(`/doctor/consultations/${id}/messages`))
}

export async function sendDoctorConsultationMessage(id, content) {
  return unwrapResult(await request.post(`/doctor/consultations/${id}/messages`, { content }))
}
