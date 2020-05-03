const checkStatus = async (res: Response) => {
  if (res.status >= 200 && res.status < 300) {
    return res.text()
  }
  const json = await res.json()
  throw Error(json.message)
}

const parseJSON = (text: string) => {
  return text ? JSON.parse(text) : {}
}

export const get = async (url: string, options: any) => {
  return fetch(url, {
    ...options,
  })
    .then(checkStatus)
    .then(parseJSON)
}
