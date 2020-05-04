import axios from 'axios'

export const get = async (url: string) => {
  return axios
    .get(url, {
      headers: {
        Accept: 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data)
}
