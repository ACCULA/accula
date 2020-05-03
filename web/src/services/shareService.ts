import axios from 'axios'

export const get = async (url: string) => {
  return axios
    .get(url, {
      headers: {
        'Access-Control-Allow-Origin': 'http://localhost:8080',
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      withCredentials: true
    })
    .then(resp => resp.data)
}
