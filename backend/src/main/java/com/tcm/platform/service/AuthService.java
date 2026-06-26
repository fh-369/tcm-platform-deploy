package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tcm.platform.dto.LoginRequest;
import com.tcm.platform.dto.LoginResponse;
import com.tcm.platform.dto.DepartmentResponse;
import com.tcm.platform.dto.DoctorApplicationRequest;
import com.tcm.platform.dto.DoctorApplicationResponse;
import com.tcm.platform.dto.RegisterRequest;
import com.tcm.platform.entity.Account;
import com.tcm.platform.entity.Department;
import com.tcm.platform.entity.PatientAccount;
import com.tcm.platform.entity.User;
import com.tcm.platform.mapper.AccountMapper;
import com.tcm.platform.mapper.DepartmentMapper;
import com.tcm.platform.mapper.PatientAccountMapper;
import com.tcm.platform.mapper.UserMapper;
import com.tcm.platform.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 患者、医生和管理员的注册登录业务。
 */
@Service
public class AuthService {

    private static final String PATIENT_ROLE = "patient";
    private static final String DOCTOR_ROLE = "doctor";
    private static final String PENDING = "PENDING";
    private static final String REJECTED = "REJECTED";

    private final AccountMapper accountMapper;
    private final PatientAccountMapper patientAccountMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            AccountMapper accountMapper,
            PatientAccountMapper patientAccountMapper,
            UserMapper userMapper,
            DepartmentMapper departmentMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.accountMapper = accountMapper;
        this.patientAccountMapper = patientAccountMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public LoginResponse registerPatient(RegisterRequest request) {
        if (findAccountByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("该用户名已存在");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordHash);
        account.setRole(PATIENT_ROLE);
        if (accountMapper.insert(account) != 1) {
            throw new IllegalStateException("账号注册失败");
        }

        PatientAccount patient = new PatientAccount();
        patient.setAccountId(account.getId());
        patient.setUsername(request.getUsername());
        patient.setPasswordHash(passwordHash);
        patient.setDisplayName(defaultDisplayName(request.getDisplayName(), request.getUsername()));
        patient.setPhone(request.getPhone());
        if (patientAccountMapper.insert(patient) != 1) {
            throw new IllegalStateException("用户资料创建失败");
        }

        return createLoginResponse(
                account.getId(),
                account.getUsername(),
                PATIENT_ROLE,
                patient.getDisplayName()
        );
    }

    public List<DepartmentResponse> listEnabledDepartments() {
        return departmentMapper.selectList(
                        Wrappers.<Department>lambdaQuery()
                                .eq(Department::getEnabled, true)
                                .orderByAsc(Department::getSortOrder)
                                .orderByAsc(Department::getId)
                )
                .stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional
    public DoctorApplicationResponse applyDoctor(DoctorApplicationRequest request) {
        String username = request.getUsername().trim();
        if (findAccountByUsername(username) != null) {
            throw new IllegalArgumentException("该用户名已存在");
        }

        Department department = departmentMapper.selectById(request.getDepartmentId());
        if (department == null || Boolean.FALSE.equals(department.getEnabled())) {
            throw new IllegalArgumentException("请选择有效科室");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        Account account = new Account();
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setRole(DOCTOR_ROLE);
        account.setEnabled(false);
        if (accountMapper.insert(account) != 1) {
            throw new IllegalStateException("医生申请提交失败");
        }

        User doctor = new User();
        doctor.setAccountId(account.getId());
        doctor.setUsername(username);
        doctor.setPasswordHash(passwordHash);
        doctor.setRole(DOCTOR_ROLE);
        doctor.setDisplayName(request.getDisplayName().trim());
        doctor.setDepartmentId(department.getId());
        doctor.setDepartment(department.getName());
        doctor.setPhone(request.getPhone().trim());
        doctor.setQualification(request.getQualification().trim());
        doctor.setProfile(trimToNull(request.getProfile()));
        doctor.setApprovalStatus(PENDING);
        if (userMapper.insert(doctor) != 1) {
            throw new IllegalStateException("医生资料创建失败");
        }

        return new DoctorApplicationResponse(account.getId(), username, PENDING);
    }

    public LoginResponse login(LoginRequest request) {
        Account account = findAccountByUsername(request.getUsername());
        if (account == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (DOCTOR_ROLE.equals(account.getRole())) {
            if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
                throw new IllegalArgumentException("用户名或密码错误");
            }
            User doctor = findUserByUsername(account.getUsername());
            validateDoctorApproval(doctor);
        }
        if (Boolean.FALSE.equals(account.getEnabled())) {
            throw new IllegalArgumentException("账号已停用，请联系管理员");
        }
        if (!DOCTOR_ROLE.equals(account.getRole())
                && !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        return createLoginResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                findDisplayName(account)
        );
    }

    public LoginResponse loginPatient(LoginRequest request) {
        LoginResponse response = login(request);
        if (!PATIENT_ROLE.equals(response.getRole())) {
            throw new IllegalArgumentException("请使用用户账号登录");
        }
        return response;
    }

    public LoginResponse loginAdmin(LoginRequest request) {
        LoginResponse response = login(request);
        if (PATIENT_ROLE.equals(response.getRole())) {
            throw new IllegalArgumentException("该账号无后台访问权限");
        }
        return response;
    }

    private Account findAccountByUsername(String username) {
        return accountMapper.selectOne(Wrappers.<Account>lambdaQuery().eq(Account::getUsername, username));
    }

    private PatientAccount findPatientByUsername(String username) {
        return patientAccountMapper.selectOne(
                Wrappers.<PatientAccount>lambdaQuery().eq(PatientAccount::getUsername, username)
        );
    }

    private User findUserByUsername(String username) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    private String findDisplayName(Account account) {
        if (PATIENT_ROLE.equals(account.getRole())) {
            PatientAccount patient = findPatientByUsername(account.getUsername());
            return patient == null ? account.getUsername() : patient.getDisplayName();
        }

        User user = findUserByUsername(account.getUsername());
        return user == null ? account.getUsername() : user.getDisplayName();
    }

    private LoginResponse createLoginResponse(Long userId, String username, String role, String displayName) {
        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(userId, username, role));
        response.setRole(role);
        response.setUserId(userId);
        response.setDisplayName(displayName);
        return response;
    }

    private String defaultDisplayName(String displayName, String username) {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }

    private void validateDoctorApproval(User doctor) {
        if (doctor == null || doctor.getApprovalStatus() == null || doctor.getApprovalStatus().isBlank()) {
            return;
        }
        if (PENDING.equals(doctor.getApprovalStatus())) {
            throw new IllegalArgumentException("医生申请正在审核中");
        }
        if (REJECTED.equals(doctor.getApprovalStatus())) {
            String note = trimToNull(doctor.getApprovalNote());
            throw new IllegalArgumentException(note == null
                    ? "医生申请未通过，请联系管理员"
                    : "医生申请未通过：" + note);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
