const SEASON_ORDER = ['春', '夏', '秋', '冬', '四季']

export function sortSeasons(seasons) {
  return [...seasons].sort((left, right) => {
    const leftIndex = SEASON_ORDER.indexOf(left)
    const rightIndex = SEASON_ORDER.indexOf(right)

    if (leftIndex !== -1 || rightIndex !== -1) {
      return (leftIndex === -1 ? SEASON_ORDER.length : leftIndex)
        - (rightIndex === -1 ? SEASON_ORDER.length : rightIndex)
    }

    return String(left).localeCompare(String(right), 'zh-Hans-CN')
  })
}

export function filterRecipes(recipes, filters = {}) {
  const season = filters.season || ''
  const constitution = filters.constitution || ''
  const keyword = (filters.keyword || '').trim().toLowerCase()

  return recipes.filter((recipe) => {
    const matchesSeason = !season || recipe.season === season
    const matchesConstitution = !constitution || recipe.constitution === constitution
    const searchableText = [
      recipe.name,
      recipe.summary,
      recipe.suitableFor,
      recipe.ingredients,
      recipe.steps,
    ].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !keyword || searchableText.includes(keyword)

    return matchesSeason && matchesConstitution && matchesKeyword
  })
}

export function paginateRecipes(recipes, current = 1, pageSize = 6) {
  const start = (current - 1) * pageSize
  return recipes.slice(start, start + pageSize)
}

export function parseRecipeList(value) {
  if (Array.isArray(value)) {
    return value
  }
  if (!value) {
    return []
  }

  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return parsed
    }
  } catch {
    // Fall back to readable chunks below.
  }

  return String(value)
    .split(/\r?\n|；|;/)
    .map((item) => item.trim())
    .filter(Boolean)
}
