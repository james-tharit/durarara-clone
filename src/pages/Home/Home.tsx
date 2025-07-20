import { useNavigate } from 'react-router';
import { checkToken } from '../../authentication/authen';
import './Home.css'
export const Home = () => {
  const navigator = useNavigate()

  function logout() {
    localStorage.removeItem('token')
    if (!checkToken()) navigator('/login')
  }

  function toChat() {
    navigator('/chat')
  }

  return (
    <div>
      <div className="home-container" >
        <div className="home-header">
          <h1 style={{ color: 'black', fontWeight: 'bold', padding: 0, margin: 0 }}>Welcome to Dollas</h1>
          <button onClick={logout} > logout</button>
        </div>
        <div className="home-body">
          <button onClick={toChat}>go to chat</button>
        </div>
      </div>
    </div >
  );
}
