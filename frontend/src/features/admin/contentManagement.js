const CONSTITUTION_OPTIONS = [
  '通用',
  '平和质',
  '气虚质',
  '阳虚质',
  '阴虚质',
  '痰湿质',
  '湿热质',
  '血瘀质',
  '气郁质',
  '特禀质',
]

export const CONTENT_CONFIGS = {
  knowledge: {
    title: '养生文章',
    singular: '文章',
    nameKey: 'title',
    imageKey: 'coverImageUrl',
    filterKey: 'category',
    filterLabel: '文章分类',
    defaultForm: {
      title: '',
      category: '',
      summary: '',
      content: '',
      tips: '',
      coverImageUrl: '',
      published: false,
    },
  },
  recipe: {
    title: '药膳管理',
    singular: '药膳',
    nameKey: 'name',
    imageKey: 'imageUrl',
    filterKey: 'season',
    filterLabel: '适用季节',
    seasonOptions: ['春', '夏', '秋', '冬', '四季'],
    constitutionOptions: CONSTITUTION_OPTIONS,
    defaultForm: {
      name: '',
      season: '',
      constitution: '',
      suitableFor: '',
      summary: '',
      ingredients: '',
      steps: '',
      imageUrl: '',
      published: false,
    },
  },
}

function trim(value) {
  return typeof value === 'string' ? value.trim() : value
}

function listToJson(value) {
  if (Array.isArray(value)) {
    return JSON.stringify(value.map(trim).filter(Boolean))
  }
  if (!value) return ''
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return JSON.stringify(parsed.map(trim).filter(Boolean))
    }
  } catch {
    // Plain text is converted below.
  }
  return JSON.stringify(String(value)
    .split(/\r?\n|；|;/)
    .map(trim)
    .filter(Boolean))
}

export function listToEditorText(value) {
  if (!value) return ''
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.join('\n')
  } catch {
    // Existing plain text remains editable.
  }
  return String(value)
}

export function normalizeContentPayload(resource, form) {
  const payload = Object.fromEntries(
    Object.keys(CONTENT_CONFIGS[resource].defaultForm)
      .map((key) => [key, trim(form[key])]),
  )
  if (resource === 'recipe') {
    payload.ingredients = listToJson(payload.ingredients)
    payload.steps = listToJson(payload.steps)
  }
  payload.published = Boolean(payload.published)
  return payload
}

export function validateContentForm(resource, form, publishing) {
  if (resource === 'knowledge') {
    if (!trim(form.title)) return { field: 'title', message: '请填写文章标题' }
    if (publishing && !trim(form.content)) {
      return { field: 'content', message: '请填写文章正文' }
    }
    return null
  }

  if (!trim(form.name)) return { field: 'name', message: '请填写药膳名称' }
  if (publishing && !trim(form.ingredients)) {
    return { field: 'ingredients', message: '请填写药膳食材' }
  }
  if (publishing && !trim(form.steps)) {
    return { field: 'steps', message: '请填写制作步骤' }
  }
  return null
}
