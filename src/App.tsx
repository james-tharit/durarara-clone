import { useEffect } from 'react'
import './App.css'
import { Dollas } from './components/Dollas'
import { authenticate } from './authentication/authen'

export const App = () => {

  useEffect(() => {
    authenticate();
  }, [])

  return (
    <div className='container'>
      <div className='logo'>
        <Dollas />
      </div>
      <div className='password-container'>
        <div className='input-container' >
          <p style={{ paddingRight: "8px" }}>Password </p>
          <input type='password' ></input>
        </div>
        <div>
          <button onClick={() => console.warn("no function")}>
            <p style={{ padding: '0px 8vh', margin: '0' }}> ENTER</p>
          </button>
        </div>

      </div>
    </div>
  )
}
