import axios from "axios"
import { useNavigate } from "react-router";

export async function authenticate(secret_keyword = '') {
  try {
    const request = await axios.post("http://localhost:3000/authenticate", { secret_keyword });
    console.log(request.data)

    if (request.data?.token) {
      localStorage.setItem('token', request.data.token)
    }

  } catch (error) {
    console.error(error);
  }
}

