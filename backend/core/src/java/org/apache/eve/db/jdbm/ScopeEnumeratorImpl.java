package org.apache.eve.db.jdbm;


import java.math.BigInteger;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.ScopeNode;
import org.apache.ldap.common.util.SingletonEnumeration;

import org.apache.eve.db.*;


/**
 * Enumerates candidates based on scope.
 * 
 */
public class ScopeEnumeratorImpl implements ScopeEnumerator
{
    /** Database used to enumerate based on scope */
    private Database db = null;
    /** Filter scope expression evaluator */
    private ScopeEvaluator evaluator = null;


    public ScopeEnumeratorImpl( Database db, ScopeEvaluator evaluator )
    {
        this.db = db;
        this.evaluator = evaluator;
    }


    /**
     * Builds an enumeration over all entries that satisfy the constraints of 
     * the scope assertion node.
     *
     * @param node the scope node 
     * @return the candidates that are within scope
     * @throws NamingException if any system indices fail
     * @see Enumerator#enumerate(ExprNode)
     */
    public NamingEnumeration enumerate( ExprNode node ) throws NamingException
    {
        final ScopeNode snode = ( ScopeNode ) node;
        final BigInteger id = db.getEntryId( snode.getBaseDn() );

        switch( snode.getScope() )
        {
        case( SearchControls.OBJECT_SCOPE ):
            final IndexRecord record = new IndexRecord();
            record.setEntryId( id );
            record.setIndexKey( snode.getBaseDn() );
            return new SingletonEnumeration( record ); 
        case( SearchControls.ONELEVEL_SCOPE ):
            return enumerateChildren( snode.getBaseDn(),
                snode.getDerefAliases().derefInSearching() );
        case( SearchControls.SUBTREE_SCOPE ):
            return enumerateDescendants( snode );
        default:
            throw new NamingException( "Unrecognized search scope!" );
        }
    }


    /**
     * Constructs an enumeration over all entries within one level scope even
     * when aliases are enabled while searching.
     * 
     * @param dn the base dn
     * @param deref whether or not we dereference while searching
     * @return the enumeration of all entries in direct or alias extended one 
     * level scope to the base
     * @throws NamingException if any failures occur while accessing system
     * indices.
     */
    private NamingEnumeration enumerateChildren( String dn, boolean deref )
        throws NamingException
    {
        Index idx = db.getHeirarchyIndex();
        final BigInteger id = db.getEntryId( dn );
        final NamingEnumeration children = idx.listIndices( id );
        
        /*
         * If alias dereferencing is not enabled while searching then we just
         * return the enumeration of the base entry's children.
         */
        if ( ! deref )
        {
           return children; 
        }

        /* ====================================================================
         * From here on Dereferencing while searching is enabled
         * ====================================================================
         *
         * Dereferencing in search is enabled so we need to wrap the child
         * listing with an assertion enumeration to weed out aliases that will
         * not be returned.  Next we need to compose an enumeration which 
         * combines the list of non-alias child entries with those entries that
         * are brought into one level scope by aliases.
         */

        // List all entries brought into one level scope at base by aliases
        idx = db.getOneAliasIndex();
        NamingEnumeration aliasIntroduced = idx.listIndices( id );
        
        // Still need to use assertion enum to weed out aliases
        NamingEnumeration nonAliasChildren = new IndexAssertionEnumeration( 
            children, new AssertNotAlias() );
        
        // Combine both into one enumeration
        NamingEnumeration [] all = {nonAliasChildren, aliasIntroduced}; 
        return new DisjunctionEnumeration( all );
    }
    

    /**
     * Constructs an enumeration over all entries within subtree scope even
     * when aliases are enabled while searching.
     * 
     * @param node the scope node
     * @return the enumeration of all entries in direct or alias extended 
     * subtree scope to the base
     * @throws NamingException if any failures occur while accessing system
     * indices.
     */
    private NamingEnumeration enumerateDescendants( final ScopeNode node )
        throws NamingException
    {
        Index idx = null;

        /*
         * If we do not dereference while searching then we simply return any
         * entry that is not a descendant of the base.
         */
        if ( ! node.getDerefAliases().derefInSearching() )
        {
            // Gets a NamingEnumeration over all elements
            idx = db.getNdnIndex();
            NamingEnumeration underlying = idx.listIndices();
            return new IndexAssertionEnumeration( underlying, 
                new AssertDescendant( node ) );
        }

        // Create an assertion to assert or evaluate an expression
        IndexAssertion assertion = new IndexAssertion()
        {
            public boolean assertCandidate( IndexRecord rec )
                throws NamingException
            {
                return evaluator.evaluate( node, rec );
            }
        };

        // Gets a NamingEnumeration over all elements
        idx = db.getNdnIndex();
        NamingEnumeration underlying = idx.listIndices();
        return new IndexAssertionEnumeration( underlying, assertion );
    }


    /**
     * Asserts an entry is a descendant.
     */
    class AssertDescendant implements IndexAssertion
    {
        /** Scope node with base and alias info */
        private final ScopeNode scope;

        
        /**
         * Creates a assertion using a ScopeNode to determine the search base.
         *
         * @param node the scope node with search base
         */
        AssertDescendant( final ScopeNode node )
        {
            scope = node;
        }


        /**
         * Returns true if the candidate with id is a descendant of the base, 
         * false otherwise.
         * 
         * @see IndexAssertion#assertCandidate(IndexRecord)
         */
        public boolean assertCandidate( IndexRecord record ) throws NamingException
        {
            String dn = db.getEntryDn( record.getEntryId() );
            return dn.endsWith( scope.getBaseDn() );
        }
    }    


    /**
     * Asserts an entry is NOT an alias.
     */
    class AssertNotAlias implements IndexAssertion
    {
        /**
         * Returns true if the candidate is not an alias, false otherwise.
         * 
         * @see IndexAssertion#assertCandidate(IndexRecord)
         */
        public boolean assertCandidate( IndexRecord record ) throws NamingException
        {
            Index aliasIdx = db.getAliasIndex();
               
            if ( null == aliasIdx.reverseLookup( record.getEntryId() ) )
            {
                return true;
            }
               
            return false;
        } 
    }
}
