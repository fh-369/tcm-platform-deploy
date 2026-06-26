import { describe, expect, it } from 'vitest'

import {
  filterRecipes,
  paginateRecipes,
  parseRecipeList,
  sortSeasons,
} from './display'

const recipes = [
  {
    name: '山药香菇鸡肉粥',
    season: '冬',
    constitution: '气虚质',
    suitableFor: '适合早餐',
    summary: '山药与鸡肉煮成温和米粥',
    ingredients: '["山药 100 克","鸡肉 100 克"]',
  },
  {
    name: '冬瓜番茄虾仁汤',
    season: '夏',
    constitution: '湿热质',
    suitableFor: '适合清爽汤菜',
    summary: '冬瓜番茄搭配虾仁',
    ingredients: '["冬瓜 250 克","虾仁 120 克"]',
  },
]

describe('recipe display helpers', () => {
  it('filters recipes by season, constitution, and keyword across common fields', () => {
    expect(filterRecipes(recipes, { season: '夏', constitution: '湿热质', keyword: '虾仁' }))
      .toEqual([recipes[1]])
  })

  it('paginates filtered recipe cards on the client side', () => {
    const result = paginateRecipes([1, 2, 3, 4, 5, 6, 7], 2, 3)

    expect(result).toEqual([4, 5, 6])
  })

  it('parses JSON recipe lists and falls back to readable text chunks', () => {
    expect(parseRecipeList('["山药 100 克","鸡肉 100 克"]')).toEqual(['山药 100 克', '鸡肉 100 克'])
    expect(parseRecipeList('第一步\n第二步')).toEqual(['第一步', '第二步'])
  })

  it('sorts season filters in spring summer autumn winter order', () => {
    expect(sortSeasons(['冬', '夏', '春', '秋', '四季'])).toEqual(['春', '夏', '秋', '冬', '四季'])
  })
})
