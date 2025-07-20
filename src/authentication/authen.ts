import axios from "axios"

export async function authenticate(secret_keyword = '') {
  try {
    const request = await axios.post("http://localhost:3000/authenticate", { secret_keyword }, { headers: { "Content-Type": "application/json" } });
    console.log(request.data)

    if (request.data?.token) {
      localStorage.setItem('token', request.data.token)
    }

  } catch (error) {
    console.error(error);
    if (axios.isAxiosError(error)) {
      if (error.status === 401) {
        throw "Wrong Secret";
      }
    }
    throw "Something went wrong :("
  }
}

export function checkToken() {
  const token = localStorage.getItem('token');
  return token ? true : false;
}

export async function healthCheck() {
  await axios.get("http://localhost:3000")
}


