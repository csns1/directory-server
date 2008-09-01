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
package org.apache.directory.mitosis.operation;


import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.name.LdapDN;

import java.util.List;


/**
 * An {@link Operation} that replaces an attribute in an entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplaceAttributeOperation extends AttributeOperation
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = -6573196586521610472L;


    /**
     * Creates a new operation that replaces the specified attribute. This 
     * constructor will not be visible out of this package, as it is 
     * only used for the deserialization process.
     * 
     * @param attribute an attribute to replace
     */
    /** No qualifier*/ ReplaceAttributeOperation( Registries registries )
    {
        super( registries, OperationType.REPLACE_ATTRIBUTE );
    }

    
    /**
     * Creates a new operation that replaces the specified attribute.
     * 
     * @param attribute an attribute to replace
     * @param csn The operation CSN
     * @param dn The associated DN
     */
    public ReplaceAttributeOperation( Registries registries, CSN csn, LdapDN dn, ServerAttribute attribute )
    {
        super( registries, OperationType.REPLACE_ATTRIBUTE, csn, dn, attribute );
    }


    /**
     * Inject the modified attribute into the server.
     * 
     * @param nexus the partition which will be modified
     * @param coreSession the current session
     */
    protected void execute1( PartitionNexus nexus, CoreSession coreSession ) throws Exception
    {
        DirectoryService ds = coreSession.getDirectoryService();
        ServerEntry serverEntry = ds.newEntry( LdapDN.EMPTY_LDAPDN );
        EntryAttribute attribute = getAttribute();
        serverEntry.put( attribute );
        List<Modification> items = ModifyOperationContext.createModItems( serverEntry, 
            ModificationOperation.REPLACE_ATTRIBUTE );

        nexus.modify( new ModifyOperationContext( coreSession, getDn(), items ) );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return super.toString() + ".replace( " + getAttribute() + " )";
    }
}
