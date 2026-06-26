export function eligibleDoctorsForDepartment(
  doctors,
  departmentId,
  generalDepartmentId,
) {
  const eligible = (doctors || []).filter(
    (doctor) =>
      doctor.enabled &&
      doctor.approvalStatus === 'APPROVED' &&
      doctor.userId,
  )

  if (!departmentId || Number(departmentId) === Number(generalDepartmentId)) {
    return eligible
  }

  return eligible.filter(
    (doctor) => Number(doctor.departmentId) === Number(departmentId),
  )
}
