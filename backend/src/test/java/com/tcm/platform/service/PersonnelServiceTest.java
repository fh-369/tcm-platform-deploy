package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.dto.DoctorProfileUpdateRequest;
import com.tcm.platform.dto.DoctorReviewRequest;
import com.tcm.platform.dto.PersonnelRecord;
import com.tcm.platform.entity.Account;
import com.tcm.platform.entity.Department;
import com.tcm.platform.entity.User;
import com.tcm.platform.mapper.AccountMapper;
import com.tcm.platform.mapper.DepartmentMapper;
import com.tcm.platform.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonnelServiceTest {

    @Test
    void listsPatientsAndDoctorsWithTheirOwnQueries() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        Page<PersonnelRecord> patientPage = new Page<>(1, 10);
        Page<PersonnelRecord> doctorPage = new Page<>(1, 10);
        when(accountMapper.selectPatientPersonnel(any(Page.class), eq("张")))
                .thenReturn(patientPage);
        when(accountMapper.selectDoctorPersonnel(any(Page.class), eq("内科"), eq("PENDING")))
                .thenReturn(doctorPage);
        PersonnelService service = new PersonnelService(accountMapper, userMapper, departmentMapper);

        IPage<PersonnelRecord> patients = service.listPatients(1, 10, "张");
        IPage<PersonnelRecord> doctors = service.listDoctors(1, 10, "内科", "PENDING");

        assertThat(patients).isSameAs(patientPage);
        assertThat(doctors).isSameAs(doctorPage);
    }

    @Test
    void updatesAnotherAccountStatusButRejectsDisablingCurrentAdministrator() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        Account doctor = account(8L, "doctor1", true);
        doctor.setRole("doctor");
        Account admin = account(1L, "admin", true);
        when(accountMapper.selectById(8L)).thenReturn(doctor);
        when(accountMapper.selectById(1L)).thenReturn(admin);
        User approvedDoctor = doctorUser(8L, "doctor1", "APPROVED");
        when(userMapper.selectOne(any())).thenReturn(approvedDoctor);
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);
        PersonnelService service = new PersonnelService(accountMapper, userMapper, departmentMapper);

        Account updated = service.updateEnabled(8L, false, "admin");

        assertThat(updated.getEnabled()).isFalse();
        verify(accountMapper).updateById(doctor);

        assertThatThrownBy(() -> service.updateEnabled(1L, false, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不能停用当前登录账号");
    }

    @Test
    void approvesPendingDoctorAndEnablesAccount() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        Account doctorAccount = account(8L, "doctor2", false);
        doctorAccount.setRole("doctor");
        User doctor = doctorUser(8L, "doctor2", "PENDING");
        User admin = doctorUser(1L, "admin", "APPROVED");
        admin.setId(1L);
        when(accountMapper.selectById(8L)).thenReturn(doctorAccount);
        when(userMapper.selectOne(any())).thenReturn(doctor, admin);
        when(userMapper.updateById(doctor)).thenReturn(1);
        when(accountMapper.updateById(doctorAccount)).thenReturn(1);
        PersonnelService service = new PersonnelService(accountMapper, userMapper, departmentMapper);

        var result = service.reviewDoctor(
                8L,
                reviewRequest("APPROVED", "资料核验通过"),
                "admin"
        );

        assertThat(result.approvalStatus()).isEqualTo("APPROVED");
        assertThat(result.enabled()).isTrue();
        assertThat(doctor.getApprovedBy()).isEqualTo(1L);
        assertThat(doctor.getApprovedAt()).isNotNull();
        verify(userMapper).updateById(doctor);
        verify(accountMapper).updateById(doctorAccount);
    }

    @Test
    void rejectsDoctorAndKeepsAccountDisabled() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        Account doctorAccount = account(8L, "doctor2", false);
        doctorAccount.setRole("doctor");
        User doctor = doctorUser(8L, "doctor2", "PENDING");
        when(accountMapper.selectById(8L)).thenReturn(doctorAccount);
        when(userMapper.selectOne(any())).thenReturn(doctor);
        when(userMapper.updateById(doctor)).thenReturn(1);
        when(accountMapper.updateById(doctorAccount)).thenReturn(1);
        PersonnelService service = new PersonnelService(accountMapper, userMapper, departmentMapper);

        var result = service.reviewDoctor(
                8L,
                reviewRequest("REJECTED", "资质信息不完整"),
                "admin"
        );

        assertThat(result.approvalStatus()).isEqualTo("REJECTED");
        assertThat(result.enabled()).isFalse();
        assertThat(doctor.getApprovalNote()).isEqualTo("资质信息不完整");
    }

    @Test
    void cannotEnableDoctorBeforeApproval() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        Account doctorAccount = account(8L, "doctor2", false);
        doctorAccount.setRole("doctor");
        when(accountMapper.selectById(8L)).thenReturn(doctorAccount);
        when(userMapper.selectOne(any())).thenReturn(doctorUser(8L, "doctor2", "PENDING"));
        PersonnelService service = new PersonnelService(accountMapper, userMapper, departmentMapper);

        assertThatThrownBy(() -> service.updateEnabled(8L, true, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("医生尚未通过审核，不能启用账号");

        verify(accountMapper, never()).updateById(any(Account.class));
    }

    @Test
    void updatesDoctorProfileWithEnabledDepartment() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        Account doctorAccount = account(8L, "doctor2", true);
        doctorAccount.setRole("doctor");
        User doctor = doctorUser(8L, "doctor2", "APPROVED");
        Department department = new Department();
        department.setId(3L);
        department.setName("中医妇科");
        department.setEnabled(true);
        when(accountMapper.selectById(8L)).thenReturn(doctorAccount);
        when(userMapper.selectOne(any())).thenReturn(doctor);
        when(departmentMapper.selectById(3L)).thenReturn(department);
        when(userMapper.updateById(doctor)).thenReturn(1);
        PersonnelService service = new PersonnelService(accountMapper, userMapper, departmentMapper);

        var result = service.updateDoctorProfile(8L, profileRequest());

        assertThat(result.departmentName()).isEqualTo("中医妇科");
        assertThat(doctor.getDepartmentId()).isEqualTo(3L);
        assertThat(doctor.getDisplayName()).isEqualTo("王医生");
        verify(userMapper).updateById(doctor);
    }

    private Account account(Long id, String username, boolean enabled) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setEnabled(enabled);
        return account;
    }

    private User doctorUser(Long accountId, String username, String approvalStatus) {
        User doctor = new User();
        doctor.setId(accountId);
        doctor.setAccountId(accountId);
        doctor.setUsername(username);
        doctor.setRole("doctor");
        doctor.setApprovalStatus(approvalStatus);
        return doctor;
    }

    private DoctorReviewRequest reviewRequest(String status, String note) {
        DoctorReviewRequest request = new DoctorReviewRequest();
        request.setApprovalStatus(status);
        request.setApprovalNote(note);
        return request;
    }

    private DoctorProfileUpdateRequest profileRequest() {
        DoctorProfileUpdateRequest request = new DoctorProfileUpdateRequest();
        request.setDisplayName("王医生");
        request.setDepartmentId(3L);
        request.setPhone("13900000000");
        request.setQualification("中医执业医师");
        request.setProfile("擅长女性健康调养");
        return request;
    }
}
