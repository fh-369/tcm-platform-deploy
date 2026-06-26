import request from './request'

function unwrapResult(response) {
  const result = response.data

  if (result?.code !== 200) {
    throw new Error(result?.message || '请求失败，请稍后重试')
  }

  return result.data
}

export async function getAdminConsultations(params) {
  return unwrapResult(await request.get('/admin/consultation', { params }))
}

export async function updateAdminConsultation(id, payload) {
  return unwrapResult(await request.put(`/admin/consultation/${id}`, payload))
}

export async function assignAdminConsultation(id, doctorId) {
  return unwrapResult(await request.put(`/admin/consultation/${id}/assignment`, { doctorId }))
}

export async function claimAdminConsultation(id) {
  return unwrapResult(await request.put(`/admin/consultation/${id}/claim`))
}

export async function updateAdminConsultationDepartment(id, departmentId) {
  return unwrapResult(await request.put(`/admin/consultation/${id}/department`, {
    departmentId,
  }))
}
