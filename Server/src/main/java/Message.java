import java.io.Serializable;

public class Message implements Serializable{
    static final long serialVersionUID = 42L;
    MessageType type;
    String string1;
    String string2;

    public Message(MessageType type, String string1){
        this.type = type;
        this.string1 = string1;
    }

    public Message(MessageType type, String string1, String string2){
        this.type = type;
        this.string1 = string1;
        this.string2 = string2;
    }
}