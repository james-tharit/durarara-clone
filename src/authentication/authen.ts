import axios from "axios"

export async function authenticate(secret_keyword = '') {
  const request = await axios.post("http://localhost:3000/authenticate", { secret_keyword });
  console.log(request.data)
}


