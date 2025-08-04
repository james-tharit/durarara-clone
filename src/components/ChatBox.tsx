import './ChatBox.css'
export const ChatBox = ({ isSender, message, id }: { isSender: boolean, message: string, id: number }) => {

  return (
    <div style={{ display: "flex", flexDirection: "column", paddingTop: "16px" }}>
      <div className="message-bubble" style={{ backgroundColor: isSender ? "#ffa500" : "#2ecc71", width: "70vw", alignSelf: isSender ? 'start' : 'end' }}>
        <pre style={{ fontSize: "14", fontWeight: "bold", color: "white", }} key={id}>{message}</pre>
      </div>
    </div>
  );
}
