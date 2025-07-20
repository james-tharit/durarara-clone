import { useNavigate } from 'react-router';
import './Chat.css'
import { initiateChat } from '../../service/llm/ollama';
import { useRef, useState } from 'react';
export const Chat = () => {
  const navigate = useNavigate();
  const prompt = useRef<HTMLTextAreaElement>(null);
  const [chats, appendChat] = useState<Array<string>>([])

  const backHome = () => {
    navigate('/');
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const message = prompt.current ? prompt.current.value : '';
    console.log("Message from useRef:", message);

    const res = await initiateChat(message)
    if (res) appendChat((prev) => [...prev, res]);
    console.log(chats)

    // Optionally clear the textarea by directly manipulating the DOM element
    if (prompt.current) {
      prompt.current.value = '';
    }
  };

  return (
    <div>
      <div className="chat-container" >
        <form onSubmit={handleSubmit}>
          <div className="chat-header">
            <p style={{ color: 'black', fontWeight: 'bold', padding: 0, margin: 0 }}>dollaschat</p>
            <button onClick={backHome}>exit</button>
          </div>
          <div className="chat-input">
            <textarea ref={prompt} />
          </div>
          <button type="submit" >post! </button>
        </form>
      </div>
      <div className="chat-body">
        {
          chats.map((message, id) => <p style={{ color: "black" }} key={id}>{message}</p>)
        }
      </div>
    </div>
  );
}
