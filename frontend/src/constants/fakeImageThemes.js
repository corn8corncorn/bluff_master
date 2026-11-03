/** 假圖類別（與後端 FakeImageCategory 對應） */
export const FAKE_IMAGE_CATEGORIES = [
  { id: 'FOOD', label: '美食' },
  { id: 'SCENERY', label: '風景' },
  { id: 'CULTURE', label: '人文' },
  { id: 'ARCHITECTURE', label: '建築' },
  { id: 'ART', label: '藝術作品' },
  { id: 'DAILY_LIFE', label: '日常生活' },
  { id: 'SHOPPING', label: '購物商品' },
]

/** 預設地區列表（API 載入失敗時使用） */
export const FAKE_IMAGE_REGIONS = [
  { id: 'TAIWAN', label: '台灣' },
  { id: 'JAPAN', label: '日本' },
  { id: 'KOREA', label: '韓國' },
  { id: 'CHINA', label: '中國' },
  { id: 'HONG_KONG', label: '香港' },
  { id: 'MACAU', label: '澳門' },
  { id: 'THAILAND', label: '泰國' },
  { id: 'VIETNAM', label: '越南' },
  { id: 'SINGAPORE', label: '新加坡' },
  { id: 'MALAYSIA', label: '馬來西亞' },
  { id: 'INDONESIA', label: '印尼' },
  { id: 'PHILIPPINES', label: '菲律賓' },
  { id: 'USA', label: '美國' },
  { id: 'CANADA', label: '加拿大' },
  { id: 'UK', label: '英國' },
  { id: 'FRANCE', label: '法國' },
  { id: 'ITALY', label: '義大利' },
  { id: 'GERMANY', label: '德國' },
  { id: 'SPAIN', label: '西班牙' },
  { id: 'SWITZERLAND', label: '瑞士' },
  { id: 'AUSTRALIA', label: '澳洲' },
  { id: 'NEW_ZEALAND', label: '紐西蘭' },
  { id: 'OTHER', label: '其他地區' },
]

export function getCategoryLabel(id, categories = FAKE_IMAGE_CATEGORIES) {
  return categories.find((c) => c.id === id)?.label || id
}

export function getRegionLabel(id, regions = FAKE_IMAGE_REGIONS) {
  return regions.find((r) => r.id === id)?.label || id
}
