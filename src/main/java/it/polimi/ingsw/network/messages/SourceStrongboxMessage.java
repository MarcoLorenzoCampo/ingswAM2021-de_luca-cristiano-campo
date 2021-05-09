package it.polimi.ingsw.network.messages;

import it.polimi.ingsw.enumerations.PossiblePlayerMessages;

public class SourceStrongboxMessage extends Message{

    public SourceStrongboxMessage(String username){
        super.setSenderUsername(username);
        super.setMessageType(PossiblePlayerMessages.SOURCE_STRONGBOX);
    }
}
