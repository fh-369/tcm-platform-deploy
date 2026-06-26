package com.tcm.platform.dto;

import com.tcm.platform.entity.Recipe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecipeAdminRequest {

    @NotBlank(message = "药膳名称不能为空")
    @Size(max = 100, message = "药膳名称不能超过 100 个字符")
    private String name;

    private String season;

    @Size(max = 50, message = "适用体质不能超过 50 个字符")
    private String constitution;

    @Size(max = 200, message = "适合人群不能超过 200 个字符")
    private String suitableFor;

    @Size(max = 500, message = "药膳摘要不能超过 500 个字符")
    private String summary;

    private String ingredients;

    private String steps;

    @Size(max = 500, message = "封面地址不能超过 500 个字符")
    private String imageUrl;

    private Boolean published;

    public Recipe toEntity() {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setSeason(season);
        recipe.setConstitution(constitution);
        recipe.setSuitableFor(suitableFor);
        recipe.setSummary(summary);
        recipe.setIngredients(ingredients);
        recipe.setSteps(steps);
        recipe.setImageUrl(imageUrl);
        recipe.setPublished(published);
        return recipe;
    }
}
