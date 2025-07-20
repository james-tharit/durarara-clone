import axios from "axios";

export const initiateChat = async (message: string): Promise<string> => {
  try {
    const request = await axios.post("http://localhost:3000/ollama", { message }, { headers: { "Content-Type": "application/json" } });
    if (request.data?.message) {
      return request.data.message
    }
    throw "invalid data"
  } catch (error) {
    console.error(error)
    throw error
  }
}
