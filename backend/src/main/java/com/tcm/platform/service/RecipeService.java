package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.entity.Recipe;
import com.tcm.platform.mapper.RecipeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 药膳公开查询与后台管理业务。
 */
@Service
public class RecipeService {

    private static final Set<String> SEASONS = Set.of("春", "夏", "秋", "冬", "四季");
    private static final Set<String> CONSTITUTIONS = Set.of(
            "通用", "平和质", "气虚质", "阳虚质", "阴虚质",
            "痰湿质", "湿热质", "血瘀质", "气郁质", "特禀质"
    );

    private final RecipeMapper recipeMapper;

    public RecipeService(RecipeMapper recipeMapper) {
        this.recipeMapper = recipeMapper;
    }

    public List<Recipe> listPublishedRecipes() {
        return recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>()
                        .eq(Recipe::getPublished, true)
                        .orderByDesc(Recipe::getCreatedAt)
        );
    }

    public Recipe getPublishedRecipe(Long id) {
        Recipe recipe = getRecipe(id);
        if (!Boolean.TRUE.equals(recipe.getPublished())) {
            throw new IllegalArgumentException("药膳不存在或尚未发布");
        }
        return recipe;
    }

    public Page<Recipe> listRecipes(
            long current,
            long size,
            String season,
            String constitution,
            Boolean published,
            String keyword
    ) {
        validatePage(current, size);

        LambdaQueryWrapper<Recipe> query = new LambdaQueryWrapper<>();
        query.eq(hasText(season), Recipe::getSeason, season)
                .eq(hasText(constitution), Recipe::getConstitution, constitution)
                .eq(published != null, Recipe::getPublished, published)
                .and(hasText(keyword), wrapper -> wrapper
                        .like(Recipe::getName, keyword)
                        .or()
                        .like(Recipe::getSummary, keyword)
                        .or()
                        .like(Recipe::getSuitableFor, keyword))
                .orderByDesc(Recipe::getCreatedAt);

        return recipeMapper.selectPage(new Page<>(current, size), query);
    }

    public Recipe getRecipe(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("药膳 ID 不能为空");
        }

        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new IllegalArgumentException("药膳不存在");
        }
        return recipe;
    }

    @Transactional
    public Recipe createRecipe(Recipe recipe) {
        validateRecipe(recipe);
        recipe.setId(null);
        normalizeRecipe(recipe);
        validateOptions(recipe);
        recipe.setPublished(Boolean.TRUE.equals(recipe.getPublished()));
        recipe.setViewCount(recipe.getViewCount() == null ? 0 : recipe.getViewCount());

        if (recipeMapper.insert(recipe) != 1) {
            throw new IllegalStateException("创建药膳失败");
        }
        return recipe;
    }

    @Transactional
    public Recipe updateRecipe(Long id, Recipe request) {
        Recipe recipe = getRecipe(id);
        validateRecipe(request);
        normalizeRecipe(request);
        validateOptions(request);

        recipe.setName(request.getName());
        recipe.setSeason(request.getSeason());
        recipe.setConstitution(request.getConstitution());
        recipe.setSuitableFor(request.getSuitableFor());
        recipe.setSummary(request.getSummary());
        recipe.setIngredients(request.getIngredients());
        recipe.setSteps(request.getSteps());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setPublished(Boolean.TRUE.equals(request.getPublished()));

        if (recipeMapper.updateById(recipe) != 1) {
            throw new IllegalStateException("更新药膳失败");
        }
        return recipe;
    }

    @Transactional
    public Recipe updatePublication(Long id, Boolean published) {
        Recipe recipe = getRecipe(id);
        if (Boolean.TRUE.equals(published)) {
            validatePublishedRecipe(recipe);
        }
        recipe.setPublished(Boolean.TRUE.equals(published));
        if (recipeMapper.updateById(recipe) != 1) {
            throw new IllegalStateException("更新药膳发布状态失败");
        }
        return recipe;
    }

    @Transactional
    public void deleteRecipe(Long id) {
        getRecipe(id);
        if (recipeMapper.deleteById(id) != 1) {
            throw new IllegalStateException("删除药膳失败");
        }
    }

    private void validateRecipe(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("药膳内容不能为空");
        }
        if (!hasText(recipe.getName())) {
            throw new IllegalArgumentException("药膳名称不能为空");
        }
        if (Boolean.TRUE.equals(recipe.getPublished()) && !hasText(recipe.getIngredients())) {
            throw new IllegalArgumentException("药膳食材不能为空");
        }
        if (Boolean.TRUE.equals(recipe.getPublished()) && !hasText(recipe.getSteps())) {
            throw new IllegalArgumentException("药膳制作步骤不能为空");
        }
    }

    private void validatePublishedRecipe(Recipe recipe) {
        if (!hasText(recipe.getIngredients())) {
            throw new IllegalArgumentException("药膳食材不能为空，无法发布");
        }
        if (!hasText(recipe.getSteps())) {
            throw new IllegalArgumentException("药膳制作步骤不能为空，无法发布");
        }
    }

    private void normalizeRecipe(Recipe recipe) {
        recipe.setName(trim(recipe.getName()));
        recipe.setSeason(trim(recipe.getSeason()));
        recipe.setConstitution(trim(recipe.getConstitution()));
        recipe.setSuitableFor(trim(recipe.getSuitableFor()));
        recipe.setSummary(trim(recipe.getSummary()));
        recipe.setIngredients(trim(recipe.getIngredients()));
        recipe.setSteps(trim(recipe.getSteps()));
        recipe.setImageUrl(trim(recipe.getImageUrl()));
    }

    private void validateOptions(Recipe recipe) {
        if (hasText(recipe.getSeason()) && !SEASONS.contains(recipe.getSeason())) {
            throw new IllegalArgumentException("请选择有效的适用季节");
        }
        if (hasText(recipe.getConstitution()) && !CONSTITUTIONS.contains(recipe.getConstitution())) {
            throw new IllegalArgumentException("请选择有效的适用体质");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validatePage(long current, long size) {
        if (current < 1) {
            throw new IllegalArgumentException("页码必须大于 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在 1 到 100 之间");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
