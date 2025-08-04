import { useNavigate } from 'react-router';
import { ChatBox } from '../../components/ChatBox';
import './Chat.css';
import { useEffect, useRef, useState } from 'react';

type ChatMessage = {
  message: string,
  isSender: boolean
}

export const Chat = () => {
  const navigate = useNavigate();
  const prompt = useRef<HTMLTextAreaElement>(null);
  const ws = useRef<WebSocket>(null)
  const [chats, appendChat] = useState<Array<ChatMessage>>([])

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

  const newChat = (message: string, isSender: boolean) => {
    appendChat((pre) => [...pre, { message, isSender }])
  }

  const sendMessage = (message: string) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      ws.current.send(message);
      newChat(message, true)
    }
  };



  useEffect(() => {
    ws.current = new WebSocket('ws://localhost:3000/chat');

    ws.current.onmessage = (m) => {
      if (m.data) {
        console.log(m.data)
        newChat(m.data.toString(), false)
      }
    }
  }, [])

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      <div className="chat-container" >
        <form onSubmit={handleSubmit}>
          <div className="chat-header">
            <p style={{ color: 'black', fontSize: "32px", fontWeight: 'bold', padding: 0, margin: 0 }}>Dollaschat</p>
            <button onClick={backHome}>exit</button>
          </div>
          <div className="chat-input">
            <textarea style={{ fontSize: "18px", borderRadius: '12px' }} ref={prompt} />
          </div>
          <button type="submit" style={{ width: "98%", margin: "16px" }} >post! </button>
        </form>
      </div>
      <div className="chat-body"  >
        {
          chats.map(({ message, isSender }, id) => (<ChatBox message={message} id={id} isSender={isSender} />))
        }
      </div>
    </div>
  );
}

