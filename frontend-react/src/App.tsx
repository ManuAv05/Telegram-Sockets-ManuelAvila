import { useState, useEffect, useRef } from 'react';
import './index.css';

interface Message {
  type: string;
  username: string;
  channel: string;
  content: string;
  activeUsers?: string[];
  availableChannels?: string[];
  targetUser?: string;
  timestamp?: number;
  imageBase64?: string;
  messageId?: string;
  userAvatars?: Record<string, string>;
}

function App() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [myAvatar, setMyAvatar] = useState<string | null>(null);
  const [userAvatars, setUserAvatars] = useState<Record<string, string>>({});
  
  const [isLogged, setIsLogged] = useState(false);
  const [socket, setSocket] = useState<WebSocket | null>(null);
  
  const [channels, setChannels] = useState<string[]>(['general']);
  const [activeChannel, setActiveChannel] = useState('general');
  const [messages, setMessages] = useState<Message[]>([]);
  const [activeUsers, setActiveUsers] = useState<string[]>([]);
  const [inputText, setInputText] = useState('');
  const [imageFile, setImageFile] = useState<string | null>(null);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const avatarInputRef = useRef<HTMLInputElement>(null);
  
  const [typingUsers, setTypingUsers] = useState<Set<string>>(new Set());
  const [targetUser, setTargetUser] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const typingTimeoutRef = useRef<Record<string, number>>({});

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const connectWebSocket = () => {
    const ws = new WebSocket('ws://localhost:8081');

    ws.onopen = () => {
      console.log('Handshake con el servidor WS');
      const authMsg = {
        type: 'AUTH',
        username: username.trim(),
        channel: '',
        content: password.trim(), 
        imageBase64: myAvatar || undefined
      };
      ws.send(JSON.stringify(authMsg));
    };

    ws.onmessage = (event) => {
      const data: Message = JSON.parse(event.data);

      if (data.type === 'AUTH_ERROR') {
        alert(data.content);
        ws.close();
      } else if (data.type === 'AUTH_SUCCESS') {
        setIsLogged(true);
        ws.send(JSON.stringify({ type: 'JOIN', username: username.trim(), channel: 'general', content: '' }));
      } else if (data.type === 'SYNC_AVATARS' && data.userAvatars) {
        setUserAvatars(data.userAvatars);
      } else if (data.type === 'CHANNELS_UPDATE' && data.availableChannels) {
        setChannels(data.availableChannels);
      } else if (data.type === 'USERS_UPDATE' && data.activeUsers) {
        setActiveUsers(data.activeUsers);
      } else if (data.type === 'TYPING') {
        if (data.username !== username.trim()) {
          handleUserTyping(data.username);
        }
      } else if (data.type === 'DELETE_MESSAGE') {
        setMessages((prev) => prev.filter(m => m.messageId !== data.messageId));
      } else if (data.type === 'MESSAGE' || data.type === 'JOIN' || data.type === 'LEAVE' || data.type === 'PRIVATE_MESSAGE') {
        setMessages((prev) => [...prev, data]);
      }
    };

    ws.onclose = () => {
      if (isLogged) {
         alert('Conexión con el servidor perdida.');
      }
      setIsLogged(false);
      setSocket(null);
    };

    setSocket(ws);
  };

  const handleUserTyping = (typingUser: string) => {
    setTypingUsers(prev => new Set(prev).add(typingUser));
    if (typingTimeoutRef.current[typingUser]) {
      clearTimeout(typingTimeoutRef.current[typingUser]);
    }
    typingTimeoutRef.current[typingUser] = window.setTimeout(() => {
      setTypingUsers(prev => {
        const newSet = new Set(prev);
        newSet.delete(typingUser);
        return newSet;
      });
    }, 2000); 
  };

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (username.trim() && password.trim()) {
      setUsername(username.trim()); // Limpiar espacios ocultos
      connectWebSocket();
    }
  };

  const changeChannel = (newChannel: string) => {
    if (newChannel === activeChannel || !socket) return;
    setMessages([]);
    setActiveChannel(newChannel);
    setTargetUser(null);
    
    const joinMsg = {
      type: 'JOIN',
      username: username,
      channel: newChannel,
      content: ''
    };
    socket.send(JSON.stringify(joinMsg));
  };

  const handleTyping = (text: string) => {
    setInputText(text);
    if (!socket || !text.trim()) return;
    
    socket.send(JSON.stringify({
      type: 'TYPING',
      username: username,
      channel: activeChannel,
      content: ''
    }));
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      if (ev.target?.result) setImageFile(ev.target.result as string);
    };
    reader.readAsDataURL(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleAvatarSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      if (ev.target?.result) setMyAvatar(ev.target.result as string);
    };
    reader.readAsDataURL(file);
  };

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if ((!inputText.trim() && !imageFile) || !socket) return;

    const isPM = targetUser !== null;
    const msg = {
      type: isPM ? 'PRIVATE_MESSAGE' : 'MESSAGE',
      username: username,
      channel: activeChannel,
      content: inputText,
      targetUser: isPM ? targetUser : undefined,
      imageBase64: imageFile || undefined,
      messageId: Math.random().toString(36).substring(2, 15) // Generate simple ID
    };
    socket.send(JSON.stringify(msg));
    setInputText('');
    setImageFile(null);
  };

  const requestDeleteMessage = (messageId?: string) => {
    if (!socket || !messageId) return;
    socket.send(JSON.stringify({
      type: 'DELETE_MESSAGE',
      username: username,
      channel: activeChannel,
      content: '', // content ignored
      messageId: messageId
    }));
  };

  const formatTime = (ts?: number) => {
    if (!ts) return '';
    const date = new Date(ts);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const startPrivateMessage = (user: string) => {
    if (user === username) return; 
    setTargetUser(user);
  };

  if (!isLogged) {
    return (
      <div className="login-container">
        <div className="login-box">
          <h1>NexCord</h1>
          <p>Te damos la bienvenida a la sala de chat en tiempo real.</p>
          
          <div style={{marginBottom: '20px', cursor:'pointer'}} onClick={()=>avatarInputRef.current?.click()}>
            <div style={{width:'80px', height:'80px', borderRadius:'50%', background:'var(--bg-chat-input)', margin:'0 auto', display:'flex', alignItems:'center', justifyContent:'center', border: '2px dashed var(--accent-color)', overflow:'hidden'}}>
               {myAvatar ? <img src={myAvatar} style={{width:'100%', height:'100%', objectFit:'cover'}} /> : <span style={{fontSize:'12px', color:'var(--text-muted)'}}>+ Poner Foto</span>}
            </div>
            <input type="file" style={{display:'none'}} ref={avatarInputRef} accept="image/*" onChange={handleAvatarSelect}/>
          </div>

          <form onSubmit={handleLogin}>
            <input 
              type="text" 
              placeholder="Nombre de Usuario" 
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoFocus
              required
            />
            <input 
              type="password" 
              placeholder="Contraseña Segura" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <button type="submit">Iniciar Sesión Segura</button>
          </form>
        </div>
      </div>
    );
  }

  const typingString = Array.from(typingUsers).join(', ');

  return (
    <div className="app-container">
      {/* SERVERS BAR */}
      <div className="servers">
        <div className="server-icon active">
          DM
        </div>
        <div className="server-icon">
          S1
        </div>
      </div>

      {/* SIDEBAR CANALES */}
      <div className="sidebar">
        <div className="sidebar-header">
          NexCord Central
        </div>
        <div className="channels-list">
          {channels.map((ch) => (
            <div 
              key={ch} 
              className={`channel-item ${(ch === activeChannel && !targetUser) ? 'active' : ''}`}
              onClick={() => changeChannel(ch)}
            >
              <span className="hash">#</span>
              <span>{ch}</span>
            </div>
          ))}
          {targetUser && (
            <div className="channel-item active" style={{marginTop: '20px', borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: '10px'}}>
              <span className="hash" style={{color:'var(--accent-color)'}}>@</span>
              <span style={{color:'var(--accent-color)', fontWeight:'bold'}}>DM: {targetUser}</span>
            </div>
          )}
        </div>
      </div>

      {/* CHAT CENTRAL */}
      <div className="chat-area">
        <div className="chat-header">
          <span className="hash">{targetUser ? '@' : '#'}</span>
          <span>{targetUser ? `Voz baja con ${targetUser}` : activeChannel}</span>
        </div>
        
        <div className="messages">
          {messages.map((m, idx) => {
            const isSystem = m.type === 'JOIN' || m.type === 'LEAVE';
            const isPrivate = m.type === 'PRIVATE_MESSAGE';
            const showDelete = m.username === username && m.messageId; // only own messages
            
            return (
              <div key={idx} className="message" style={{ opacity: isSystem ? 0.7 : 1, background: isPrivate ? 'rgba(88, 101, 242, 0.05)' : 'transparent', borderLeft: isPrivate ? '2px solid var(--accent-color)' : 'none', position: 'relative' }}>
                {!isSystem && (
                  userAvatars[m.username] ? 
                    <img src={userAvatars[m.username]} className="avatar" style={{objectFit: 'cover', background: isPrivate ? '#3A449F' : ''}} />
                    :
                    <div className="avatar" style={isPrivate ? {background: '#3A449F'} : {}}>
                      {m.username.charAt(0).toUpperCase()}
                    </div>
                )}
                {isSystem && (
                  <div className="avatar" style={{ background: '#2B2D31', fontSize: '20px', color: '#828A9A' }}>
                     →
                  </div>
                )}
                <div className="msg-content" style={{flex: 1}}>
                  <div className="msg-header">
                    <span className="msg-username">{m.username} {isPrivate && <span style={{fontSize:'10px', background:'var(--accent-color)', padding:'2px 6px', borderRadius:'4px', marginLeft:'6px'}}>SUSURRO</span>}</span>
                    <span className="msg-time">{formatTime(m.timestamp)}</span>
                    {showDelete && !isSystem && (
                       <button onClick={() => requestDeleteMessage(m.messageId)} style={{marginLeft:'auto', background:'red', color:'white', fontSize:'10px', border:'none', borderRadius:'4px', padding:'2px 6px', cursor:'pointer', opacity: 0.8}}>Borrar</button>
                    )}
                  </div>
                  <div className="msg-text">
                    {isSystem ? <em style={{color: '#949BA4'}}>{m.content}</em> : m.content}
                    {m.imageBase64 && (
                      <div style={{ marginTop: '8px' }}>
                        <img src={m.imageBase64} alt="attachment" style={{ maxWidth: '300px', maxHeight:'300px', borderRadius:'8px', cursor:'pointer' }} />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        <div className="chat-input-wrapper">
          {imageFile && (
            <div style={{ position:'absolute', top:'-60px', left:'24px', background:'var(--bg-chat-input)', padding:'8px', borderRadius:'8px', display:'flex', gap:'10px', alignItems:'center' }}>
              <img src={imageFile} style={{width:'40px', height:'40px', objectFit:'cover', borderRadius:'4px'}}/>
              <button onClick={()=>setImageFile(null)} style={{background:'red', color:'white', border:'none', borderRadius:'50%', width:'24px', height:'24px', cursor:'pointer'}}>X</button>
            </div>
          )}
          {typingUsers.size > 0 && (
            <div style={{ position: 'absolute', top: '-20px', left: '24px', fontSize: '12px', color: 'var(--text-bright)', fontWeight:'bold', animation:'fadeInMsg 0.3s' }}>
              <span style={{display:'inline-block', width:'8px', height:'8px', background:'var(--accent-color)', borderRadius:'50%', marginRight:'6px', animation:'float 1s infinite alternate'}}></span>
              {typingString} {typingUsers.size === 1 ? 'está escribiendo...' : 'están escribiendo...'}
            </div>
          )}
          <form className="chat-input" onSubmit={sendMessage}>
            <button type="button" onClick={()=>fileInputRef.current?.click()} style={{background:'transparent', border:'none', color:'var(--text-muted)', fontSize:'20px', cursor:'pointer', marginRight:'12px', paddingBottom:'4px'}}>+</button>
            <input 
              type="file" 
              ref={fileInputRef} 
              style={{display: 'none'}} 
              accept="image/*"
              onChange={handleFileChange}
            />
            <input 
              type="text" 
              placeholder={targetUser ? `Susurrar a @${targetUser}...` : `Enviar mensaje a #${activeChannel}`} 
              value={inputText}
              onChange={(e) => handleTyping(e.target.value)}
            />
          </form>
        </div>
      </div>

      {/* PANEL DERECHO (USUARIOS) */}
      <div className="users-panel">
        <div className="users-header">
          Alineación — {activeUsers.length}
        </div>
        <div className="users-list">
          {activeUsers.map((u) => (
            <div key={u} className="user-item" onClick={() => startPrivateMessage(u)}>
              <div className="user-avatar-wrapper">
                {userAvatars[u] ? (
                  <img src={userAvatars[u]} className="user-avatar" style={{objectFit: 'cover'}} />
                ) : (
                  <div className="user-avatar">
                    {u.charAt(0).toUpperCase()}
                  </div>
                )}
                <div className="status-dot"></div>
              </div>
              <span className="user-name">{u} {u === username && "(Tú)"}</span>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}

export default App;
