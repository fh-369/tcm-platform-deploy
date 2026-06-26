package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tcm.platform.dto.LoginRequest;
import com.tcm.platform.dto.LoginResponse;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @BeforeAll
    static void initializeTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "account-test"), Account.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "patient-test"), PatientAccount.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "user-test"), User.class);
    }

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PatientAccountMapper patientAccountMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Test
    void registerPatientEncodesPasswordUsesUsernameAsDefaultDisplayNameAndReturnsToken() {
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("patient123")).thenReturn("encoded-password");
        when(accountMapper.insert(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(9L);
            return 1;
        });
        when(patientAccountMapper.insert(any(PatientAccount.class))).thenAnswer(invocation -> {
            return 1;
        });
        when(jwtUtil.generateToken(9L, "patient1", "patient")).thenReturn("patient-token");
        AuthService service = service();
        RegisterRequest request = registerRequest("patient1", "patient123", " ", "13800000000");

        LoginResponse response = service.registerPatient(request);

        assertThat(response.getToken()).isEqualTo("patient-token");
        assertThat(response.getRole()).isEqualTo("patient");
        assertThat(response.getDisplayName()).isEqualTo("patient1");
        verify(passwordEncoder).encode("patient123");
        verify(patientAccountMapper).insert(any(PatientAccount.class));
    }

    @Test
    void registerPatientRejectsDuplicateUsernameBeforeEncodingOrInsert() {
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new Account());

        assertThatThrownBy(() -> service().registerPatient(registerRequest("patient1", "patient123", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该用户名已存在");

        verify(passwordEncoder, never()).encode(any());
        verify(accountMapper, never()).insert(any());
        verify(patientAccountMapper, never()).insert(any());
    }

    @Test
    void registerPatientFailsWhenMapperDoesNotInsertRecord() {
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("patient123")).thenReturn("encoded-password");
        when(accountMapper.insert(any(Account.class))).thenReturn(0);

        assertThatThrownBy(() -> service().registerPatient(registerRequest("patient1", "patient123", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("账号注册失败");

        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void loginPatientRejectsIncorrectPassword() {
        Account account = account(5L, "patient1", "encoded-password", "patient");
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> service().loginPatient(loginRequest("patient1", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void loginAdminReturnsRoleAndDisplayName() {
        Account account = account(1L, "admin", "encoded-password", "admin");
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash("encoded-password");
        admin.setRole("admin");
        admin.setDisplayName("系统管理员");
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(admin);
        when(passwordEncoder.matches("admin123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "admin", "admin")).thenReturn("admin-token");

        LoginResponse response = service().loginAdmin(loginRequest("admin", "admin123"));

        assertThat(response.getToken()).isEqualTo("admin-token");
        assertThat(response.getRole()).isEqualTo("admin");
        assertThat(response.getDisplayName()).isEqualTo("系统管理员");
    }

    @Test
    void loginRejectsDisabledAccount() {
        Account account = account(5L, "patient1", "encoded-password", "patient");
        account.setEnabled(false);
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);

        assertThatThrownBy(() -> service().login(loginRequest("patient1", "patient123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号已停用，请联系管理员");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void applyDoctorCreatesDisabledAccountAndPendingDoctorProfile() {
        Department department = department(2L, "internal-medicine", "中医内科", true);
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(departmentMapper.selectById(2L)).thenReturn(department);
        when(passwordEncoder.encode("doctor123")).thenReturn("encoded-doctor-password");
        when(accountMapper.insert(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(12L);
            return 1;
        });
        when(userMapper.insert(any(User.class))).thenReturn(1);

        DoctorApplicationResponse response = service().applyDoctor(doctorApplicationRequest());

        assertThat(response.accountId()).isEqualTo(12L);
        assertThat(response.username()).isEqualTo("doctor2");
        assertThat(response.approvalStatus()).isEqualTo("PENDING");
        verify(accountMapper).insert(org.mockito.ArgumentMatchers.argThat(account ->
                "doctor".equals(account.getRole()) && Boolean.FALSE.equals(account.getEnabled())
        ));
        verify(userMapper).insert(org.mockito.ArgumentMatchers.argThat(user ->
                user.getDepartmentId().equals(2L)
                        && "PENDING".equals(user.getApprovalStatus())
                        && "13800000001".equals(user.getPhone())
        ));
    }

    @Test
    void applyDoctorRejectsDisabledDepartmentBeforeCreatingAccount() {
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(departmentMapper.selectById(2L))
                .thenReturn(department(2L, "internal-medicine", "中医内科", false));

        assertThatThrownBy(() -> service().applyDoctor(doctorApplicationRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择有效科室");

        verify(accountMapper, never()).insert(any());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void loginDoctorExplainsPendingAndRejectedReviewStatus() {
        Account account = account(12L, "doctor2", "encoded-password", "doctor");
        account.setEnabled(false);
        User doctor = new User();
        doctor.setUsername("doctor2");
        doctor.setApprovalStatus("PENDING");
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(doctor);
        when(passwordEncoder.matches("doctor123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> service().login(loginRequest("doctor2", "doctor123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("医生申请正在审核中");

        doctor.setApprovalStatus("REJECTED");
        doctor.setApprovalNote("资质信息不完整");

        assertThatThrownBy(() -> service().login(loginRequest("doctor2", "doctor123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("医生申请未通过：资质信息不完整");
    }

    private AuthService service() {
        return new AuthService(
                accountMapper,
                patientAccountMapper,
                userMapper,
                departmentMapper,
                passwordEncoder,
                jwtUtil
        );
    }

    private RegisterRequest registerRequest(String username, String password, String displayName, String phone) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setDisplayName(displayName);
        request.setPhone(phone);
        return request;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private PatientAccount patient(Long id, String username, String passwordHash, String displayName) {
        PatientAccount account = new PatientAccount();
        account.setId(id);
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setDisplayName(displayName);
        return account;
    }

    private Account account(Long id, String username, String passwordHash, String role) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setRole(role);
        account.setEnabled(true);
        return account;
    }

    private DoctorApplicationRequest doctorApplicationRequest() {
        DoctorApplicationRequest request = new DoctorApplicationRequest();
        request.setUsername("doctor2");
        request.setPassword("doctor123");
        request.setDisplayName("李医生");
        request.setDepartmentId(2L);
        request.setPhone("13800000001");
        request.setQualification("中医执业医师");
        request.setProfile("擅长常见内科调养");
        return request;
    }

    private Department department(Long id, String code, String name, boolean enabled) {
        Department department = new Department();
        department.setId(id);
        department.setCode(code);
        department.setName(name);
        department.setEnabled(enabled);
        return department;
    }
}
