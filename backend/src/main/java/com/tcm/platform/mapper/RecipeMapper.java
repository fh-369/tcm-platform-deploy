package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {

    @Select("SELECT COUNT(*) FROM recipes WHERE published = 1")
    long countPublished();
}
