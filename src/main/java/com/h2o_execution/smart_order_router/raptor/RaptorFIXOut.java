package com.h2o_execution.smart_order_router.raptor;

import lombok.extern.slf4j.Slf4j;
import quickfix.*;

@Slf4j
public class RaptorFIXOut extends MessageCracker implements Application
{

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

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType
    {

    }
}
