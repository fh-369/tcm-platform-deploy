import { describe, expect, it } from 'vitest'

import {
  CONTENT_CONFIGS,
  normalizeContentPayload,
  validateContentForm,
} from './contentManagement'

describe('admin content management rules', () => {
  it('keeps recipe season options in the expected order', () => {
    expect(CONTENT_CONFIGS.recipe.seasonOptions).toEqual(['春', '夏', '秋', '冬', '四季'])
  })

  it('normalizes recipe list inputs into JSON arrays', () => {
    const payload = normalizeContentPayload('recipe', {
      name: ' 山药粥 ',
      ingredients: '山药\n粳米',
      steps: '洗净\n慢煮',
    })

    expect(payload.name).toBe('山药粥')
    expect(payload.ingredients).toBe('["山药","粳米"]')
    expect(payload.steps).toBe('["洗净","慢煮"]')
  })

  it('requires the fields needed to publish an article', () => {
    expect(validateContentForm('knowledge', {
      title: '睡眠节律',
      content: '',
    }, true)).toEqual({
      field: 'content',
      message: '请填写文章正文',
    })
  })

  it('allows an incomplete recipe to be saved as a draft when it has a name', () => {
    expect(validateContentForm('recipe', {
      name: '山药粥',
      ingredients: '',
      steps: '',
    }, false)).toBeNull()
  })
})
