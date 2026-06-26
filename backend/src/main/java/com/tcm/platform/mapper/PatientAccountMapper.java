package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.PatientAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientAccountMapper extends BaseMapper<PatientAccount> {
}
