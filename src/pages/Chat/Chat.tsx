import { useNavigate } from 'react-router';
import './Chat.css'
import { useEffect, useRef, useState } from 'react';
export const Chat = () => {
  const navigate = useNavigate();
  const prompt = useRef<HTMLTextAreaElement>(null);

  const ws = useRef<WebSocket>(null)
  const [chats, appendChat] = useState<Array<string>>([])

  const backHome = () => {
    navigate('/');
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const message = prompt.current ? prompt.current.value : '';
    console.log("Message from useRef:", message);

    sendMessage(message)

    // Optionally clear the textarea by directly manipulating the DOM element
    if (prompt.current) {
      prompt.current.value = '';
    }
  };

  const sendMessage = (message: string) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(message);
    }
  };


  useEffect(() => {
    ws.current = new WebSocket('ws://localhost:3000/chat');

    ws.current.onmessage = (m) => {
      if (m.data) {
        console.log(m.data)
        appendChat((prev) => [...prev, m.data.toString()])
      }
    }
  }, [])

  return (
    <div>
      <div className="chat-container" >
        <form onSubmit={handleSubmit}>
          <div className="chat-header">
            <p style={{ color: 'black', fontWeight: 'bold', padding: 0, margin: 0 }}>dollaschat</p>
            <button onClick={backHome}>exit</button>
          </div>
          <div className="chat-input">
            <textarea disabled={ws.current?.OPEN ? false : true} ref={prompt} />
          </div>
          <button type="submit" >post! </button>
        </form>
      </div>
      <div className="chat-body">
        {
          chats.map((message, id) => <pre style={{ color: "black" }} key={id} dangerouslySetInnerHTML={{ __html: message }} />)
        }
      </div>
    </div>
  );
}
