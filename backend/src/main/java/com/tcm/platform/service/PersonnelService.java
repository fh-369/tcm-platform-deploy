package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tcm.platform.dto.DoctorProfileResponse;
import com.tcm.platform.dto.DoctorProfileUpdateRequest;
import com.tcm.platform.dto.DoctorReviewRequest;
import com.tcm.platform.dto.DoctorReviewResponse;
import com.tcm.platform.dto.PersonnelRecord;
import com.tcm.platform.entity.Account;
import com.tcm.platform.entity.Department;
import com.tcm.platform.entity.User;
import com.tcm.platform.mapper.AccountMapper;
import com.tcm.platform.mapper.DepartmentMapper;
import com.tcm.platform.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PersonnelService {

    private static final String DOCTOR_ROLE = "doctor";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final AccountMapper accountMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    public PersonnelService(
            AccountMapper accountMapper,
            UserMapper userMapper,
            DepartmentMapper departmentMapper
    ) {
        this.accountMapper = accountMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
    }

    public IPage<PersonnelRecord> listPatients(long current, long size, String keyword) {
        return accountMapper.selectPatientPersonnel(new Page<>(current, size), normalizeKeyword(keyword));
    }

    public IPage<PersonnelRecord> listDoctors(
            long current,
            long size,
            String keyword,
            String approvalStatus
    ) {
        return accountMapper.selectDoctorPersonnel(
                new Page<>(current, size),
                normalizeKeyword(keyword),
                normalizeStatus(approvalStatus)
        );
    }

    @Transactional
    public Account updateEnabled(Long accountId, boolean enabled, String currentUsername) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!enabled && account.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        if (enabled && DOCTOR_ROLE.equals(account.getRole())) {
            User doctor = findDoctorByAccountId(accountId);
            if (doctor == null || !APPROVED.equals(doctor.getApprovalStatus())) {
                throw new IllegalArgumentException("医生尚未通过审核，不能启用账号");
            }
        }

        account.setEnabled(enabled);
        if (accountMapper.updateById(account) != 1) {
            throw new IllegalStateException("账号状态更新失败");
        }
        return account;
    }

    @Transactional
    public DoctorReviewResponse reviewDoctor(
            Long accountId,
            DoctorReviewRequest request,
            String currentUsername
    ) {
        Account account = requireDoctorAccount(accountId);
        User doctor = requireDoctor(accountId);
        String status = request.getApprovalStatus();
        if (status.equals(doctor.getApprovalStatus())) {
            throw new IllegalArgumentException("该医生申请已经是当前审核状态");
        }

        String note = trimToNull(request.getApprovalNote());
        if (REJECTED.equals(status) && note == null) {
            throw new IllegalArgumentException("驳回申请时请填写审核备注");
        }

        doctor.setApprovalStatus(status);
        doctor.setApprovalNote(note);
        if (APPROVED.equals(status)) {
            User reviewer = findUserByUsername(currentUsername);
            if (reviewer == null) {
                throw new IllegalArgumentException("审核管理员不存在");
            }
            doctor.setApprovedAt(LocalDateTime.now());
            doctor.setApprovedBy(reviewer.getId());
            account.setEnabled(true);
        } else {
            doctor.setApprovedAt(null);
            doctor.setApprovedBy(null);
            account.setEnabled(false);
        }

        if (userMapper.updateById(doctor) != 1 || accountMapper.updateById(account) != 1) {
            throw new IllegalStateException("医生审核状态更新失败");
        }

        return new DoctorReviewResponse(
                accountId,
                doctor.getApprovalStatus(),
                account.getEnabled(),
                doctor.getApprovalNote()
        );
    }

    @Transactional
    public DoctorProfileResponse updateDoctorProfile(
            Long accountId,
            DoctorProfileUpdateRequest request
    ) {
        requireDoctorAccount(accountId);
        User doctor = requireDoctor(accountId);
        Department department = departmentMapper.selectById(request.getDepartmentId());
        if (department == null || Boolean.FALSE.equals(department.getEnabled())) {
            throw new IllegalArgumentException("请选择有效科室");
        }

        doctor.setDisplayName(request.getDisplayName().trim());
        doctor.setDepartmentId(department.getId());
        doctor.setDepartment(department.getName());
        doctor.setPhone(request.getPhone().trim());
        doctor.setQualification(request.getQualification().trim());
        doctor.setProfile(trimToNull(request.getProfile()));
        if (userMapper.updateById(doctor) != 1) {
            throw new IllegalStateException("医生资料更新失败");
        }

        return new DoctorProfileResponse(
                accountId,
                doctor.getDisplayName(),
                doctor.getDepartmentId(),
                doctor.getDepartment(),
                doctor.getPhone(),
                doctor.getQualification(),
                doctor.getProfile()
        );
    }

    private String normalizeKeyword(String keyword) {
        return trimToNull(keyword);
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return null;
        }
        if (!"PENDING".equals(normalized)
                && !APPROVED.equals(normalized)
                && !REJECTED.equals(normalized)) {
            throw new IllegalArgumentException("审核状态无效");
        }
        return normalized;
    }

    private Account requireDoctorAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || !DOCTOR_ROLE.equals(account.getRole())) {
            throw new IllegalArgumentException("医生账号不存在");
        }
        return account;
    }

    private User requireDoctor(Long accountId) {
        User doctor = findDoctorByAccountId(accountId);
        if (doctor == null || !DOCTOR_ROLE.equals(doctor.getRole())) {
            throw new IllegalArgumentException("医生资料不存在");
        }
        return doctor;
    }

    private User findDoctorByAccountId(Long accountId) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getAccountId, accountId)
        );
    }

    private User findUserByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
