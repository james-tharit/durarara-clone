import { useNavigate } from 'react-router';
import { checkAuthenticated } from '../../authentication/authen';
import './Home.css'
export const Home = () => {
  const navigator = useNavigate()
  function logout() {
    localStorage.removeItem('token')
    if (!checkAuthenticated()) navigator('/login')
  }

  return (
    <div>
      <div className="chat-container" >
        <h1 style={{ color: 'black', fontWeight: 'bold', padding: 0, margin: 0 }}>Welcome to Dollas</h1>
        <button onClick={logout} > logout</button>
      </div>
    </div >
  );
}
