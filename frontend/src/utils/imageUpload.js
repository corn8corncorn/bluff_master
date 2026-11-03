import heic2any from 'heic2any'

const MAX_DIMENSION = 1080
const JPEG_QUALITY = 0.82
const SKIP_COMPRESS_BYTES = 350000

function isHeicFile(file) {
  const type = (file.type || '').toLowerCase()
  const name = (file.name || '').toLowerCase()
  return (
    type === 'image/heic' ||
    type === 'image/heif' ||
    type === 'image/heic-sequence' ||
    type === 'image/heif-sequence' ||
    name.endsWith('.heic') ||
    name.endsWith('.heif')
  )
}

function jpegFileName(originalName) {
  const base = originalName.replace(/\.(heic|heif|png|webp|gif)$/i, '')
  return `${base || 'photo'}.jpg`
}

async function convertHeicToJpeg(file) {
  const converted = await heic2any({
    blob: file,
    toType: 'image/jpeg',
    quality: 0.85,
  })
  const blob = Array.isArray(converted) ? converted[0] : converted
  return new File([blob], jpegFileName(file.name), {
    type: 'image/jpeg',
    lastModified: file.lastModified,
  })
}

function compressWithCanvas(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()

    img.onload = () => {
      URL.revokeObjectURL(url)
      let { width, height } = img
      const max = MAX_DIMENSION

      if (width > max || height > max) {
        if (width >= height) {
          height = Math.round((height * max) / width)
          width = max
        } else {
          width = Math.round((width * max) / height)
          height = max
        }
      }

      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        resolve(file)
        return
      }
      ctx.drawImage(img, 0, 0, width, height)

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error(`無法壓縮圖片「${file.name}」`))
            return
          }
          resolve(
            new File([blob], jpegFileName(file.name), {
              type: 'image/jpeg',
              lastModified: file.lastModified,
            })
          )
        },
        'image/jpeg',
        JPEG_QUALITY
      )
    }

    img.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(file)
    }

    img.src = url
  })
}

async function normalizeImageFile(file) {
  if (isHeicFile(file)) {
    file = await convertHeicToJpeg(file)
  }

  if (file.size <= SKIP_COMPRESS_BYTES && file.type === 'image/jpeg') {
    return file
  }

  return compressWithCanvas(file)
}

/**
 * 轉換 HEIC 並壓縮尺寸，減少手機上傳流量與時間
 */
export async function prepareFilesForUpload(files, onProgress) {
  const prepared = []
  const total = files.length

  for (let i = 0; i < files.length; i++) {
    onProgress?.({ phase: 'prepare', current: i + 1, total })
    try {
      prepared.push(await normalizeImageFile(files[i]))
    } catch (error) {
      console.error('圖片處理失敗:', files[i].name, error)
      if (isHeicFile(files[i])) {
        throw new Error(`無法處理 HEIC 圖片「${files[i].name}」，請改用 JPG 或 PNG`)
      }
      throw new Error(`無法處理圖片「${files[i].name}」`)
    }
  }

  return prepared
}
