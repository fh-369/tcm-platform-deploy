package com.tcm.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.common.Result;
import com.tcm.platform.dto.PublicationRequest;
import com.tcm.platform.dto.RecipeAdminRequest;
import com.tcm.platform.entity.Recipe;
import com.tcm.platform.service.RecipeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

/**
 * 医生和管理员管理药膳的接口。
 */
@RestController
@RequestMapping("/api/admin/recipe")
public class RecipeAdminController {

    private final RecipeService recipeService;

    public RecipeAdminController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public Result<Page<Recipe>> listRecipes(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String constitution,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(
                recipeService.listRecipes(current, size, season, constitution, published, keyword)
        );
    }

    @GetMapping("/{id}")
    public Result<Recipe> getRecipe(@PathVariable Long id) {
        return Result.success(recipeService.getRecipe(id));
    }

    @PostMapping
    public Result<Recipe> createRecipe(@Valid @RequestBody RecipeAdminRequest request) {
        return Result.success("药膳创建成功", recipeService.createRecipe(request.toEntity()));
    }

    @PutMapping("/{id}")
    public Result<Recipe> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeAdminRequest request
    ) {
        return Result.success("药膳更新成功", recipeService.updateRecipe(id, request.toEntity()));
    }

    @PutMapping("/{id}/publication")
    public Result<Recipe> updatePublication(
            @PathVariable Long id,
            @Valid @RequestBody PublicationRequest request
    ) {
        return Result.success(
                Boolean.TRUE.equals(request.published()) ? "药膳已发布" : "药膳已取消发布",
                recipeService.updatePublication(id, request.published())
        );
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return Result.success("药膳删除成功", null);
    }
}
