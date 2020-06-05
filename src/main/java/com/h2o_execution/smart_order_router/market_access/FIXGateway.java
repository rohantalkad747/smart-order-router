package com.h2o_execution.smart_order_router.market_access;

import lombok.extern.slf4j.Slf4j;
import quickfix.*;
import quickfix.field.ApplVerID;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.MessageCracker;
import quickfix.fix42.Reject;

@Slf4j
public class FIXGateway extends MessageCracker implements quickfix.Application
{
    private static final String CONFIG = "C:/Users/Rohan/git/smart_order_router/src/main/resources/gatewayserver.properties";
    private OrderActionsMediator orderActionsMediator;

    public FIXGateway()
    {
    }

    public static void main(String[] args) throws Exception
    {
        SessionSettings settings = new SessionSettings(CONFIG);
        Application application = new FIXGateway();
        FileStoreFactory fileStoreFactory = new FileStoreFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        FileLogFactory fileLogFactory = new FileLogFactory(settings);
        SocketAcceptor socketAcceptor = new SocketAcceptor(application, fileStoreFactory,
                settings, fileLogFactory, messageFactory);
        socketAcceptor.start();
    }

    private ApplVerID getApplVerID(Session session, Message message)
    {
        String beginString = session.getSessionID().getBeginString();
        if (FixVersions.BEGINSTRING_FIXT11.equals(beginString))
        {
            return new ApplVerID(ApplVerID.FIX50);
        }
        else
        {
            return MessageUtils.toApplVerID(beginString);
        }
    }

    public void sendMessage(SessionID sessionID, Message message)
    {
        try
        {
            Session session = Session.lookupSession(sessionID);
            if (session == null)
            {
                throw new SessionNotFound(sessionID.toString());
            }
            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            if (dataDictionaryProvider != null)
            {
                dataDictionaryProvider.getApplicationDataDictionary(getApplVerID(session, message)).validate(message, true);
            }
            session.send(message);
        }
        catch (SessionNotFound | FieldNotFound | IncorrectTagValue | IncorrectDataFormat e)
        {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType
    {
        crack(message, sessionId);
    }

    @Override
    public void onMessage(Reject message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
    {
        orderActionsMediator.fireRejectionEvent(message);
    }

    /**
     * Routes executions to active SORs.
     *
     * @param message
     * @param sessionID
     */
    @Override
    public void onMessage(ExecutionReport message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
    {
        super.onMessage(message, sessionID);
    }

    @Override
    public void onCreate(SessionID sessionId)
    {

    }

    @Override
    public void onLogon(SessionID sessionId)
    {

    }

    @Override
    public void onLogout(SessionID sessionId)
    {
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId)
    {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon
    {
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend
    {
    }
}
