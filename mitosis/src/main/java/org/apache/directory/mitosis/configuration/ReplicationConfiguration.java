/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.mitosis.configuration;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNFactory;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.DefaultCSNFactory;
import org.apache.directory.mitosis.common.DefaultUUIDFactory;
import org.apache.directory.mitosis.common.UUID;
import org.apache.directory.mitosis.common.UUIDFactory;
import org.apache.directory.mitosis.service.ReplicationInterceptor;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.mitosis.store.derby.DerbyReplicationStore;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration for {@link ReplicationInterceptor}.  This configuration can be
 * used by calling {@link ReplicationInterceptor#setConfiguration(ReplicationConfiguration)}.
 * 
 * @org.apache.xbean.XBean
 *
 * @author The Apache Directory Project Team
 */
public class ReplicationConfiguration
{
    public static final int DEFAULT_LOG_MAX_AGE = 7;
    public static final int DEFAULT_REPLICATION_INTERVAL = 5;
    public static final int DEFAULT_RESPONSE_TIMEOUT = 60;
    public static final int DEFAULT_SERVER_PORT = 7846;

    /** The logger */
    private static Logger log = LoggerFactory.getLogger( ReplicationConfiguration.class );

    /** The server identifier */ 
    private String replicaId;
    
    /** Default values for the communication part */
    private int serverPort = DEFAULT_SERVER_PORT;
    private int responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
    private int replicationInterval = DEFAULT_REPLICATION_INTERVAL;

    /** List of connected replicas */
    private final Set<Replica> peerReplicas = new HashSet<Replica>();
    
    /** Factories */
    private UUIDFactory uuidFactory = new DefaultUUIDFactory();
    private CSNFactory csnFactory = new DefaultCSNFactory();
    private ReplicationStore store = new DerbyReplicationStore();
    
    /** The longest period of time before a stored entry is removed from storage */
    private int logMaxAge = DEFAULT_LOG_MAX_AGE; // a week (days)


    /**
     * Creates a new instance with default properties except for
     * the {@link ReplicaId} of the service and the  the list of peer
     * {@link Replica}s.  You can set these properties by calling
     * {@link #setReplicaId(ReplicaId)} and {@link #setPeerReplicas(Set)}
     * respectively.
     */
    public ReplicationConfiguration()
    {
    }

    /**
     * Returns the TCP/IP port number that a {@link ReplicationInterceptor}
     * listens to.  The default value is {@link #DEFAULT_SERVER_PORT}. 
     */
    public int getServerPort()
    {
        return serverPort;
    }


    /**
     * Sets the TCP/IP port number that a {@link ReplicationInterceptor}
     * listens to.  The default value is {@link #DEFAULT_SERVER_PORT}.
     */
    public void setServerPort( int serverPort )
    {
        this.serverPort = serverPort;
    }

    /**
     * Returns the response timeout value (seconds) for each sent message
     * during the communication between replicas.  If any response message
     * is not received within this timeout, the connection is closed and
     * reestablished.  The default value is {@link #DEFAULT_RESPONSE_TIMEOUT}.
     */
    public int getResponseTimeout()
    {
        return responseTimeout;
    }

    /**
     * Sets the response timeout value (seconds) for each sent message
     * during the communication between replicas.  If any response message
     * is not received within this timeout, the connection is closed and
     * reestablished.  The default value is {@link #DEFAULT_RESPONSE_TIMEOUT}.
     */
    public void setResponseTimeout( int responseTimeout )
    {
        this.responseTimeout = responseTimeout;
    }

    /**
     * Returns the replication data exchange interval (seconds) between two
     * replicas. The default value is {@link #DEFAULT_REPLICATION_INTERVAL}.
     * 
     * @return <tt>0</tt> if automatic replication is disabled
     */
    public int getReplicationInterval() {
        return replicationInterval;
    }


    /**
     * Sets the replication data exchange interval (seconds) between two
     * replicas. The default value is {@link #DEFAULT_REPLICATION_INTERVAL}.
     * 
     * @param replicationInterval <tt>0</tt> or below to disable automatic replication.
     */
    public void setReplicationInterval( int replicationInterval )
    {
        if( replicationInterval < 0 )
        {
            replicationInterval = 0;
        }
        
        this.replicationInterval = replicationInterval;
    }


    /**
     * Returns the {@link CSNFactory} for generating {@link CSN}s.
     * The default factory is {@link DefaultCSNFactory}.
     */
    public CSNFactory getCsnFactory()
    {
        return csnFactory;
    }


    /**
     * Sets the {@link CSNFactory} for generating {@link CSN}s.
     * The default factory is {@link DefaultCSNFactory}.
     */
    public void setCsnFactory( CSNFactory csnFactory )
    {
        this.csnFactory = csnFactory;
    }

    /**
     * Adds the specified {@link Replica} to the remote peer replica list.
     */
    public void addPeerReplica( Replica peer )
    {
        assert peer != null;
        peerReplicas.add( peer );
    }

    /**
     * Removed the specified {@link Replica} from the remote peer replica list.
     */
    public void removePeerReplica( Replica peer )
    {
        assert peer != null;
        peerReplicas.remove( peer );
    }

    /**
     * Clears the remote peer replica list.
     */
    public void removeAllPeerReplicas()
    {
        peerReplicas.clear();
    }

    /**
     * Returns the remote peer replica list.
     */
    public Set<Replica> getPeerReplicas()
    {
        Set<Replica> result = new HashSet<Replica>();
        result.addAll( peerReplicas );
        return result;
    }


    /**
     * Sets the remote peer replica list.
     */
    public void setPeerReplicas( Set<Replica> replicas )
    {
        assert replicas != null;

        Set<Replica> normalizedReplicas = new HashSet<Replica>();
        
        for ( Object replica:replicas )
        {
            if ( replica instanceof Replica )
            {
                normalizedReplicas.add( ( Replica ) replica );
            }
            else
            {
                normalizedReplicas.add( new Replica( replica.toString() ) );
            }
        }
        
        peerReplicas.clear();
        peerReplicas.addAll( normalizedReplicas );
    }

    /**
     * Returns the ID of the replica this configuration is configuring.
     */
    public String getReplicaId()
    {
        return replicaId;
    }

    /**
     * Sets the ID of the replica this configuration is configuring.
     */
    public void setReplicaId( String replicaId )
    {
        this.replicaId = replicaId;
    }

    /**
     * Returns the {@link ReplicationStore} which stores the change log
     * of the replica this configuration is configuring.  The default
     * implementation is {@link DerbyReplicationStore}.
     */
    public ReplicationStore getStore()
    {
        return store;
    }

    /**
     * Sets the {@link ReplicationStore} which stores the change log
     * of the replica this configuration is configuring.  The default
     * implementation is {@link DerbyReplicationStore}.
     */
    public void setStore( ReplicationStore store )
    {
        this.store = store;
    }

    /**
     * Returns the {@link UUIDFactory} which generates {@link UUID}s for
     * new directory entries.  The default implementation is
     * {@link DefaultUUIDFactory}.
     */
    public UUIDFactory getUuidFactory()
    {
        return uuidFactory;
    }

    /**
     * Sets the {@link UUIDFactory} which generates {@link UUID}s for
     * new directory entries.  The default implementation is
     * {@link DefaultUUIDFactory}.
     */
    public void setUuidFactory( UUIDFactory uuidFactory )
    {
        this.uuidFactory = uuidFactory;
    }

    /**
     * Returns the maximum age (days) of change logs stored in
     * {@link ReplicationStore}.  Any change logs and deleted entries
     * older than this value will be purged periodically.  The default value
     * is {@link #DEFAULT_LOG_MAX_AGE}.
     */
    public int getLogMaxAge()
    {
        return logMaxAge;
    }

    /**
     * Sets the maximum age (days) of change logs stored in
     * {@link ReplicationStore}.  Any change logs and deleted entries
     * older than this value will be purged periodically.  The default value
     * is {@link #DEFAULT_LOG_MAX_AGE}.
     */
    public void setLogMaxAge( int logMaxAge )
    {
        if ( logMaxAge <= 0 )
        {
            throw new ReplicationConfigurationException( "logMaxAge: " + logMaxAge );
        }

        this.logMaxAge = logMaxAge;
    }


    /**
     * Validate Mitosis configuration.
     * 
     * We check that the configuration file contains valid
     * parameters :<br/>
     *  - a replicaId<br/>
     *  - a valid server port (between 0 and 65535)<br/>
     *  - a valid response timeout ( > 0 )<br/>
     *  - a uuidFactory<br/>
     *  - a CSN factory<br/>
     *  - a store (derby)<br/>
     *  - a list of valid replica, none of them being equal
     *  to the replicaId 
     *
     * @throws ReplicationConfigurationException If the configuration file is invalid
     */
    public void validate() throws ReplicationConfigurationException
    {
        if ( replicaId == null )
        {
            log.error( "The replicaId is missing" );
            throw new ReplicationConfigurationException( "Replica ID is not specified." );
        }

        if ( ( serverPort < 0 ) || ( serverPort > 65535 ) )
        {
            log.error( "The replica port is not between 0 and 65535" );
            throw new ReplicationConfigurationException( "Server port is invalid: " + serverPort );
        }

        if ( responseTimeout <= 0 )
        {
            log.error( "The replica responsetimeout is negative" );
            throw new ReplicationConfigurationException( "Invalid response timeout: " + responseTimeout );
        }

        if ( uuidFactory == null )
        {
            log.error( "The UUID factory has not been declared" );
            throw new ReplicationConfigurationException( "UUID factory is not specified." );
        }

        if ( csnFactory == null )
        {
            log.error( "The CSN factory has not been declared" );
            throw new ReplicationConfigurationException( "CSN factory is not specified." );
        }

        if ( store == null )
        {
            log.error( "The store has not been declared" );
            throw new ReplicationConfigurationException( "Replication store is not specified." );
        }

        if ( peerReplicas.size() == 0 )
        {
            log.error( "The replicas peer list is empty" );
            throw new ReplicationConfigurationException( "No peer replicas" );
        }

        // Check the peer replicas.
        // We should check that no replica has the same Id, and that we don't
        // have two replicas on the same server with the same port
        Set<String> ids = new TreeSet<String>();
        Map<String, Integer> servers = new HashMap<String, Integer>();

        // Initialize the set with this server replicaId
        ids.add( replicaId );

        // And store the local inetadress
        servers.put( "localhost", serverPort );
        servers.put( "127.0.0.1", serverPort );

        try
        {
            servers.put( StringTools.lowerCase( InetAddress.getByName( "127.0.0.1" ).getHostName() ), serverPort );
        }
        catch ( UnknownHostException uhe )
        {
            // Should never occurs with 127.0.0.1
            throw new ReplicationConfigurationException( "Unknown host name" );
        }

        for ( Replica peer:peerReplicas )
        {
            if ( ids.contains( peer.getId() ) )
            {
                log.error( "Peer replica ID '{}' has already been declared.", peer.getId() );
                throw new ReplicationConfigurationException( "Peer replica ID '" + peer.getId()
                    + "' has already been declared." );
            }

            // Now check that we don't already have a replica on a server with the same port 
            String replicaServer = StringTools.lowerCase( peer.getAddress().getHostName() );
            int replicaPort = peer.getAddress().getPort();

            if ( servers.containsKey( replicaServer ) )
            {
                int peerPort = servers.get( replicaServer );

                if ( replicaPort == peerPort )
                {
                    log.error(
                        "The replica in the peer list has already been declared on the server {} with the port {}",
                        replicaServer, peerPort );
                    throw new ReplicationConfigurationException( "Replication store is not specified." );
                }
            }

            servers.put( replicaServer, replicaPort );
        }
    }
}
