package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.entity.Recipe;
import com.tcm.platform.mapper.RecipeMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                Recipe.class
        );
    }

    @Mock
    private RecipeMapper recipeMapper;

    @Test
    void listPublishedRecipesOnlyQueriesPublishedContent() {
        RecipeService service = new RecipeService(recipeMapper);
        when(recipeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());

        service.listPublishedRecipes();

        ArgumentCaptor<LambdaQueryWrapper<Recipe>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recipeMapper).selectList(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getCustomSqlSegment()).contains("published");
    }

    @Test
    void createRecipeDefaultsToDraftWithZeroViews() {
        RecipeService service = new RecipeService(recipeMapper);
        when(recipeMapper.insert(any(Recipe.class))).thenReturn(1);
        Recipe recipe = recipe("山药粥", "[\"山药\",\"粳米\"]", "[\"煮粥\"]");

        Recipe created = service.createRecipe(recipe);

        assertThat(created.getPublished()).isFalse();
        assertThat(created.getViewCount()).isZero();
    }

    @Test
    void listRecipesFiltersBySeasonConstitutionPublishedAndKeyword() {
        RecipeService service = new RecipeService(recipeMapper);
        when(recipeMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        service.listRecipes(1, 10, "春", "平和质", true, "山药");

        ArgumentCaptor<LambdaQueryWrapper<Recipe>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recipeMapper).selectPage(any(IPage.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getCustomSqlSegment())
                .contains("season", "constitution", "published", "name", "summary", "OR");
    }

    @Test
    void deleteRecipeDeletesExistingRecipe() {
        RecipeService service = new RecipeService(recipeMapper);
        when(recipeMapper.selectById(9L)).thenReturn(recipe("待删除药膳", "[\"食材\"]", "[\"步骤\"]"));
        when(recipeMapper.deleteById(9L)).thenReturn(1);

        service.deleteRecipe(9L);

        verify(recipeMapper).deleteById(9L);
    }

    @Test
    void updatePublicationOnlyChangesPublishedState() {
        RecipeService service = new RecipeService(recipeMapper);
        Recipe stored = recipe("山药粥", "[\"山药\"]", "[\"煮粥\"]");
        stored.setId(6L);
        stored.setPublished(false);
        when(recipeMapper.selectById(6L)).thenReturn(stored);
        when(recipeMapper.updateById(stored)).thenReturn(1);

        Recipe updated = service.updatePublication(6L, true);

        assertThat(updated.getPublished()).isTrue();
        assertThat(updated.getName()).isEqualTo("山药粥");
        verify(recipeMapper).updateById(stored);
    }

    @Test
    void createRecipeNormalizesSeasonConstitutionAndText() {
        RecipeService service = new RecipeService(recipeMapper);
        when(recipeMapper.insert(any(Recipe.class))).thenReturn(1);
        Recipe recipe = recipe("  山药粥  ", "[\"山药\"]", "[\"煮粥\"]");
        recipe.setSeason(" 冬 ");
        recipe.setConstitution(" 气虚质 ");

        Recipe created = service.createRecipe(recipe);

        assertThat(created.getName()).isEqualTo("山药粥");
        assertThat(created.getSeason()).isEqualTo("冬");
        assertThat(created.getConstitution()).isEqualTo("气虚质");
    }

    @Test
    void createRecipeRejectsUnsupportedSeason() {
        RecipeService service = new RecipeService(recipeMapper);
        Recipe recipe = recipe("山药粥", "[\"山药\"]", "[\"煮粥\"]");
        recipe.setSeason("雨季");

        assertThatThrownBy(() -> service.createRecipe(recipe))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择有效的适用季节");
    }

    private Recipe recipe(String name, String ingredients, String steps) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setIngredients(ingredients);
        recipe.setSteps(steps);
        return recipe;
    }
}
