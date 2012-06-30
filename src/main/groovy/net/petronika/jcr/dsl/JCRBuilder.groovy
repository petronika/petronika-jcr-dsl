package net.petronika.jcr.dsl

import javax.jcr.*
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.*
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.JcrUtils

class JCRBuilder extends BuilderSupport {

	/**
	 * JCR path separator
	 */
	static final String PATH_SEPARATOR = "/"

	/**
	 * Default node primary type name
	 */
	static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured"

	/**
	 * Node identifier prefix
	 */
	static final String ID_PREFIX = "#"

	private Session session = null
	private QueryManager queryManager = null

	JCRBuilder(Session session, Node parentNode = null) {
		super()
		assert session
		this.session = session
		queryManager = session.workspace.queryManager
	}

	/*
	 * BuilderSupport overrides
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object path) {
		return createJCRNode(path)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object path, Object primaryTypeName) {
		return createJCRNode(path, primaryTypeName)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object path, Map properties) {
		return createJCRNode(path, null, properties)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object createNode(Object path, Map properties, Object primaryTypeName) {
		return createJCRNode(path, primaryTypeName, properties)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setParent(Object parent, Object child) {
	}

	/**
	 * Creates new or returns existing node
	 *  
	 * @param path Node identifier (with prefix #) / name / absolute path / relative path
	 * @return Corresponding node
	 */
	protected Node createJCRNode(String path, String primaryTypeName = null, Map properties = null) {
		if ( path.startsWith(ID_PREFIX) ) {
			String id = path[1..path.length()-1]
			return session.getNodeByIdentifier(id)
		}
		if ( path == PATH_SEPARATOR ) {
			return session.rootNode
		}
		Node parent
		if ( path.startsWith(PATH_SEPARATOR) ) {
			path = path[1..path.length()-1]
			parent = session.rootNode
		} else {
			parent = getCurrent() ?: session.rootNode
		}
		Node node = parent.hasNode(path) ? parent.getNode(path) : primaryTypeName ? parent.addNode(path, primaryTypeName) : parent.addNode(path)
		properties?.each { key, val ->
			node.setProperty(key, val)
		}
		return node
	}

	/*
	 * Node creation methods
	 * 
	 * Common syntax:
	 * 	$(String node, [String primaryTypeName], [Map properties], [Closure closure])
	 * 		node				mandatory identifier (with prefix #) / name / absolute path / relative path (always before primaryTypeName)
	 * 		primaryTypeName		optional node primary type name (always after name)
	 * 		properties			optional node properties
	 * 		closure				optional closure (always last)
	 * Order of arguments does not matter except for the above cases.
	 */

	Node $(Object node) {
		return invokeMethod(getNodePath(node))
	}

	Node $(Object node, Closure closure) {
		return invokeMethod(getNodePath(node), closure)
	}

	Node $(Object node, String primaryNodeTypeName) {
		return invokeMethod(getNodePath(node), primaryNodeTypeName)
	}

	Node $(Object node, String primaryNodeTypeName, Closure closure) {
		return invokeMethod(getNodePath(node), [primaryNodeTypeName, closure])
	}

	Node $(Object node, String primaryNodeTypeName, Map properties) {
		return invokeMethod(getNodePath(node), [primaryNodeTypeName, properties])
	}

	Node $(Object node, String primaryNodeTypeName, Map properties, Closure closure) {
		return invokeMethod(getNodePath(node), [primaryNodeTypeName, properties, closure])
	}

	Node $(Object node, Map properties) {
		return invokeMethod(getNodePath(node), properties)
	}

	Node $(Object node, Map properties, Closure closure) {
		return invokeMethod(getNodePath(node), [properties, closure])
	}

	Node $(Map properties, Object node) {
		return invokeMethod(getNodePath(node), properties)
	}

	Node $(Map properties, Object node, Closure closure) {
		return invokeMethod(getNodePath(node), [properties, closure])
	}

	Node $(Map properties, Object node, String primaryNodeTypeName) {
		return invokeMethod(getNodePath(node), [primaryNodeTypeName, properties])
	}

	Node $(Map properties, Object node, String primaryNodeTypeName, Closure closure) {
		return invokeMethod(getNodePath(node), [primaryNodeTypeName, properties, closure])
	}

	protected String getNodePath(Object node) {
		if ( node instanceof Node ) {
			return node.path
		}
		return node.toString()
	}

	/*
	 * Session utilities
	 */

	/**
	 * Returns the root node of the workspace, "/".
	 * 
     * @return The root node of the workspace: a <code>{@link Node}</code>
     * object.
     * @see javax.jcr.Session#getRootNode()
	 */
	Node $root() {
		return session.getRootNode()
	}

    /**
     * Returns the node at the specified absolute path in the workspace.
     *
     * @param absPath An absolute path.
     * @return the specified <code>Node</code>.
     * @see javax.jcr.Session#getNode(String)
     */
	Node $nodeByPath(String absPath) {
		return session.getNode(absPath)
	}

    /**
     * Returns the node specified by the given identifier. Applies to both
     * referenceable and non-referenceable nodes.
     *
     * @param id An identifier.
     * @return A <code>Node</code>.
     * @see javax.jcr.Session#getNodeByIdentifier(String)
     */
	Node $nodeById(String id) {
		return session.getNodeByIdentifier(id)
	}

    /**
     * Returns <code>true</code> if a node exists at <code>absPath</code> and
     * this <code>Session</code> has read access to it; otherwise returns
     * <code>false</code>.
     *
     * @param absPath An absolute path.
     * @return a <code>boolean</code>
     * @see javax.jcr.Session#nodeExists(String)
     */
	boolean $nodeExists(String absPath) {
		return session.nodeExists(absPath)
	}

    /**
     * Validates all pending changes currently recorded in this
     * <code>Session</code>. If validation of <i>all</i> pending changes
     * succeeds, then this change information is cleared from the
     * <code>Session</code>.
     * <p>
     * If the <code>save</code> occurs outside a transaction, the changes are
     * <i>dispatched</i> and <i>persisted</i>. Upon being persisted the changes
     * become potentially visible to other <code>Sessions</code> bound to the
     * same persitent workspace.
     * <p>
     * If the <code>save</code> occurs within a transaction, the changes are
     * <i>dispatched</i> but are not <i>persisted</i> until the transaction is
     * committed.
     * <p>
     * If validation fails, then no pending changes are dispatched and they
     * remain recorded on the <code>Session</code>. There is no best-effort or
     * partial <code>save</code>.
     */
	void $save() {
		session.save()
	}

	/*
	 * Finders
	 */

	Node $findByName(String name, String primaryTypeName = null, Node ancestor = null) {
		if ( !primaryTypeName ) {
			primaryTypeName = DEFAULT_PRIMARY_TYPE
		}
		String query
		if ( !ancestor ) {
			query = "select * from [${primaryTypeName}] where name() = '${name}'"
		} else {
			query = "select * from [${primaryTypeName}] where name() = '${name}' and isdescendantnode([${ancestor.path}])"
		}
		return $queryFirstNode(query)
	}

	Node $findByName(String name, Node ancestor) {
		return $findByName(name, null, ancestor)
	}

	/*
	 * Query utilities
	 */

	Node $queryFirstNode(String query) {
		Query q = queryManager.createQuery(query, Query.JCR_SQL2)
		q.offset = 0
		q.limit = 1
		NodeIterator nodes = q.execute().getNodes()
		return nodes.hasNext() ? nodes.nextNode() : null
	}

	Iterable<Node> $queryNodes(String query, int offset, int limit) {
		Query q = queryManager.createQuery(query, Query.JCR_SQL2)
		q.offset = offset
		q.limit = limit
		return JcrUtils.getNodes(q.execute())
	}

	Iterable<Node> $queryNodes(String query) {
		Query q = queryManager.createQuery(query, Query.JCR_SQL2)
		return JcrUtils.getNodes(q.execute())
	}

	Row $queryFirstRow(String query) {
		Query q = queryManager.createQuery(query, Query.JCR_SQL2)
		q.offset = 0
		q.limit = 1
		RowIterator rows = q.execute().getRows()
		return rows.hasNext() ? rows.nextRow() : null
	}

	Iterable<Row> $queryRows(String query, int offset, int limit) {
		Query q = queryManager.createQuery(query, Query.JCR_SQL2)
		q.offset = offset
		q.limit = limit
		return JcrUtils.getRows(q.execute())
	}

	Iterable<Row> $queryRows(String query) {
		Query q = queryManager.createQuery(query, Query.JCR_SQL2)
		return JcrUtils.getRows(q.execute())
	}
}