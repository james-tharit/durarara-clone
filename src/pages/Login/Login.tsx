import { useEffect, useRef, useState } from 'react'
import './Login.css'
import { Dollas } from '../../components/Dollas.tsx'
import { authenticate, checkAuthenticated } from '../../authentication/authen'
import { useNavigate } from 'react-router'

export const Login = () => {
  // This ensures the *entire* string consists only of alphanumeric characters.
  const alphanumericRegex = /^[a-zA-Z0-9]+$/;

  const navigation = useNavigate();
  const keywordRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (checkAuthenticated()) navigation('/')
  }, [])


  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const keyword = keywordRef.current?.value ?? '';

    if (keyword.trim() == '') { setError('can not be empty'); return; }
    if (!alphanumericRegex.test(keyword.trim())) { setError('can not have special characters'); return; }
    setError('');
    await authenticate(keyword);
    checkAuthenticated();
  }

  return (
    <div className='container'>
      <div className='logo'>
        <Dollas />
      </div>
      <div className='password-container'>
        <form onSubmit={submit}>
          <div className='input-container' >
            <p style={{ paddingRight: "8px" }}>Password </p>
            <input ref={keywordRef} name='keyword' type='password' ></input>
          </div>
          <div className='button-container'>
            <button type='submit' onClick={() => console.warn("no function")}>
              <p style={{ padding: '0px 8vh', margin: '0' }}> ENTER</p>
            </button>
            {error ? <p style={{ fontSize: 'xx-small', color: 'red', fontWeight: 'bold' }}>{error}</p> : <></>}
          </div>
        </form>
      </div>
    </div>
  )
}
