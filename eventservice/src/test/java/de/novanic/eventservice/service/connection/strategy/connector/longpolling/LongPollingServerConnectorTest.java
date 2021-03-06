/*
 * GWTEventService
 * Copyright (c) 2014 and beyond, GWTEventService Committers
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 * Other licensing for GWTEventService may also be possible on request.
 * Please view the license.txt of the project for more information.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package de.novanic.eventservice.service.connection.strategy.connector.longpolling;

import de.novanic.eventservice.service.EventServiceException;
import de.novanic.eventservice.service.connection.strategy.connector.ConnectionStrategyServerConnector;
import de.novanic.eventservice.service.connection.strategy.connector.ConnectionStrategyServerConnectorTest;
import de.novanic.eventservice.client.event.domain.Domain;
import de.novanic.eventservice.client.event.domain.DomainFactory;
import de.novanic.eventservice.service.registry.user.UserInfo;
import de.novanic.eventservice.test.testhelper.DummyEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author sstrohschein
 *         <br>Date: 16.03.2010
 *         <br>Time: 20:40:34
 */
@RunWith(JUnit4.class)
public class LongPollingServerConnectorTest extends ConnectionStrategyServerConnectorTest
{
    @Test
    public void testListen() throws Exception {
        final Domain theDomain = DomainFactory.getDomain("test_domain");
        final UserInfo theUserInfo = new UserInfo("test_user");

        ConnectionStrategyServerConnector theLongPollingListener = new LongPollingServerConnector(createConfiguration(0, 2000, 90000));

        ListenRunnable theListenRunnable = new ListenRunnable(theLongPollingListener, theUserInfo);
        Thread theListenThread = new Thread(theListenRunnable);
        theListenThread.start();

        theUserInfo.addEvent(theDomain, new DummyEvent());

        theListenThread.join();

        ListenResult theListenResult = theListenRunnable.getListenResult();
        assertEquals(1, theListenResult.getEvents().size());
        assertTrue(theListenResult.getDuration() < 500);
    }

    @Test
    public void testListen_Min_Waiting() throws Exception {
        final Domain theDomain = DomainFactory.getDomain("test_domain");
        final UserInfo theUserInfo = new UserInfo("test_user");

        ConnectionStrategyServerConnector theLongPollingListener = new LongPollingServerConnector(createConfiguration(500, 2000, 90000));

        ListenRunnable theListenRunnable = new ListenRunnable(theLongPollingListener, theUserInfo);
        Thread theListenThread = new Thread(theListenRunnable);
        theListenThread.start();

        theUserInfo.addEvent(theDomain, new DummyEvent());

        theListenThread.join();

        ListenResult theListenResult = theListenRunnable.getListenResult();
        assertEquals(1, theListenResult.getEvents().size());
        assertTrue(theListenResult.getDuration() > 400); //could be get a little bit lesser than the specified min. waiting time (sleep) caused by accuracies of the OS and JDK implementations. 
    }

    @Test
    public void testListen_Min_Waiting_Interrupted() throws Exception {
        final Domain theDomain = DomainFactory.getDomain("test_domain");
        final UserInfo theUserInfo = new UserInfo("test_user");
        ConnectionStrategyServerConnector theLongPollingListener = new LongPollingServerConnector(createConfiguration(1000, 2000, 90000));

        ListenRunnable theListenRunnable = new ListenRunnable(theLongPollingListener, theUserInfo);
        Thread theListenThread = new Thread(theListenRunnable);

        Date theStartTime = new Date();
        theListenThread.start();

        theUserInfo.addEvent(theDomain, new DummyEvent());

        //wait to ensure that the connector is waiting
        Thread.sleep(200);

        //interrupt min. waiting
        theListenThread.interrupt();

        theListenThread.join();

        EventServiceException theOccurredException = theListenRunnable.getOccurredException();
        assertNotNull(theOccurredException);
        assertTrue(theOccurredException.getCause() instanceof InterruptedException);

        Date theEndTime = new Date();
        assertTrue((theEndTime.getTime() - theStartTime.getTime()) < 1000);
    }

    @Test
    public void testListen_Max_Waiting() throws Exception {
        final UserInfo theUserInfo = new UserInfo("test_user");

        ConnectionStrategyServerConnector theLongPollingListener = new LongPollingServerConnector(createConfiguration(0, 500, 90000));

        ListenRunnable theListenRunnable = new ListenRunnable(theLongPollingListener, theUserInfo);
        Thread theListenThread = new Thread(theListenRunnable);
        theListenThread.start();
        theListenThread.join();

        ListenResult theListenResult = theListenRunnable.getListenResult();
        assertEquals(0, theListenResult.getEvents().size());
        assertTrue(theListenResult.getDuration() >= 400);
    }

    @Test
    public void testGetEncoding() throws Exception {
        testGetEncoding(LongPollingServerConnector.class);
    }

    @Test
    public void testGetEncoding_Error() throws Exception {
        testGetEncoding_Error(LongPollingServerConnector.class);
    }
}
