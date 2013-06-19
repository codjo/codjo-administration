package net.codjo.administration.server.audit.mad;
import net.codjo.administration.server.audit.AdministrationLogFileMock;
import net.codjo.agent.UserId;
import net.codjo.mad.server.MadConnectionManager;
import net.codjo.mad.server.handler.Handler;
import net.codjo.mad.server.handler.HandlerContext;
import net.codjo.security.common.api.User;
import net.codjo.test.common.LogString;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class HandlerExecutionSpyTest {
    private LogString log = new LogString();
    private HandlerExecutionSpy handlerExecutionSpy;


    @Before
    public void setUp() {
        handlerExecutionSpy = new HandlerExecutionSpy(new AdministrationLogFileMock(log));
    }


    @Test
    public void test_handlerStartedStopped() throws Exception {
        Handler handler = mock(Handler.class);
        stub(handler.getId()).toReturn("AnHandlerId");

        MadConnectionManager madConnectionManager = mock(MadConnectionManager.class);
        stub(madConnectionManager.countUnusedConnections()).toReturn(5);
        stub(madConnectionManager.countUsedConnections()).toReturn(7);

        HandlerContext handlerContext = mock(HandlerContext.class);
        User user = mock(User.class);
        UserId userId = mock(UserId.class);
        stub(handlerContext.getUserProfil()).toReturn(user);
        stub(handlerContext.getConnectionManager()).toReturn(madConnectionManager);
        stub(user.getId()).toReturn(userId);
        stub(userId.encode()).toReturn("encodedUserId");

        handlerExecutionSpy.handlerStarted(handler, handlerContext);
        handlerExecutionSpy.handlerStopped(handler, handlerContext);

        log.assertContent("write(CONNECTIONS, ), write(HANDLER, Temps Total)");
    }
}
